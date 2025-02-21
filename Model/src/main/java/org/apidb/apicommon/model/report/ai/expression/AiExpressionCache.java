package org.apidb.apicommon.model.report.ai.expression;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import org.apache.log4j.Logger;
import org.apidb.apicommon.model.report.ai.expression.GeneRecordProcessor.ExperimentInputs;
import org.apidb.apicommon.model.report.ai.expression.GeneRecordProcessor.GeneSummaryInputs;
import org.gusdb.fgputil.cache.disk.OnDiskCache;
import org.gusdb.fgputil.cache.disk.OnDiskCache.EntryNotCreatedException;
import org.gusdb.fgputil.functional.FunctionalInterfaces.ConsumerWithException;
import org.gusdb.fgputil.functional.FunctionalInterfaces.FunctionWithException;
import org.gusdb.fgputil.functional.FunctionalInterfaces.PredicateWithException;
import org.gusdb.fgputil.functional.FunctionalInterfaces.SupplierWithException;
import org.gusdb.wdk.model.WdkModel;
import org.json.JSONException;
import org.json.JSONObject;

public class AiExpressionCache {

  private static Logger LOG = Logger.getLogger(AiExpressionCache.class);

  // cache location
  private static final String CACHE_DIR_PROP_NAME = "AI_EXPRESSION_CACHE_DIR";
  private static final String DEFAULT_TMP_CACHE_SUBDIR = "expressionCache";

  // catch characteristics
  private static final long DEFAULT_TIMEOUT_MILLIS = 5000;
  private static final long DEFAULT_POLL_FREQUENCY_MILLIS = 500;

  // cache filenames
  private static final String CACHED_DATA_FILE = "cached_data.txt";
  private static final String CACHE_DIGEST_FILE = "digest.txt";

  // returned JSON props and values
  private static final String CACHE_STATUS = "cacheStatus"; // hit or miss
  private static final String CACHE_HIT = "hit";
  private static final String HIT_RESULT = "expressionSummary"; // if hit, will have result
  private static final String CACHE_MISS = "miss";
  private static final String MISS_REASON = "reason";  // if miss, will have reason

  // status messages
  private static class LookupException extends Exception {
    public static final LookupException EXPIRED_ENTRY = new LookupException("Expired entry");
    public static final LookupException CORRUPTED_ENTRY = new LookupException("Corrupted entry");
    public static final LookupException MISSING_ENTRY = new LookupException("Missing entry");
    private LookupException(String msg) { super(msg); }
    public JSONObject toJson() {
      return new JSONObject()
          .put(CACHE_STATUS, CACHE_MISS)
          .put(MISS_REASON, getMessage());
    }
  }

  // singleton pattern
  private static AiExpressionCache _instance;

  public static synchronized AiExpressionCache getInstance(WdkModel wdkModel) throws IOException {
    if (_instance == null) {
      _instance = new AiExpressionCache(wdkModel);
    }
    else if (_instance._wdkModel != wdkModel) {
      // callers should always use the same model
      throw new IllegalStateException("Attempt to get instance with different model than previously used.");
    }
    return _instance;
  }

  // private fields
  private final WdkModel _wdkModel;
  private final OnDiskCache _cache;

  // constructor
  public AiExpressionCache(WdkModel wdkModel) throws IOException {
    _wdkModel = wdkModel;

    Path cacheParentDir = Optional
        .ofNullable(_wdkModel.getProperties().get(CACHE_DIR_PROP_NAME))
        .map(Paths::get)
        .orElse(Paths.get(_wdkModel.getModelConfig().getWdkTempDir().toString(), DEFAULT_TMP_CACHE_SUBDIR));

    _cache = new OnDiskCache(cacheParentDir, DEFAULT_TIMEOUT_MILLIS, DEFAULT_POLL_FREQUENCY_MILLIS);
  }

  /**
   * Tries to read a gene summary from the cache without populating if absent.
   *
   * @param summaryInputs inputs for cache lookup
   * @return response JSON (indicating cache hit or not with data or miss reason respectively)
   */
  public JSONObject readSummary(GeneSummaryInputs summaryInputs) {
    try {
      return _cache.visitContent(summaryInputs.getGeneId(),
          geneDir -> getValidSummary(geneDir, summaryInputs));
    }
    catch (LookupException e) {
      return e.toJson();
    }
    catch (EntryNotCreatedException e) {
      return LookupException.MISSING_ENTRY.toJson();
    }
    catch (Exception e) {
      // any other exception is a 500
      throw e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e);
    }
  }

  /**
   * Confirms experiment descriptors are present and up to date with the inputs; if so,
   * confirms summary is up to date with the inputs; if so, returns it.  If anything is
   * missing or out of date, returns cache-miss JSON.
   *
   * @param geneDir directory for the summary entry
   * @param summaryInputs inputs
   * @return response JSON (indicating cache hit or not with data or miss reason respectively)
   * @throws Exception lookup or other exception if unable to find or validate cached data
   */
  private JSONObject getValidSummary(Path geneDir, GeneSummaryInputs summaryInputs) throws Exception {

    // check for existence of valid cache entries for each experiment
    // if any are missing or expired, exception will be thrown causing a cache miss
    for (ExperimentInputs datasetInput : summaryInputs.getExperimentsWithData()) {
      _cache.visitContent(datasetInput.getCacheKey(), experimentDir -> {
        return getValidStoredData(experimentDir, datasetInput.getDigest());
      });
    }

    // once all experiment values are confirmed, check for valid summary entry
    JSONObject summary = getValidStoredData(geneDir, summaryInputs.getDigest());
    return new JSONObject()
        .put(CACHE_STATUS, CACHE_HIT)
        .put(HIT_RESULT, summary);
  }

  /**
   * Checks an entry for a valid digest and readable data file; if valid and present, returns
   * parsed JSON data
   *
   * @param entryDir directory of the entry (could be summary or experiment)
   * @param computedDigest expected digest; mismatch indicates cache entry is expired
   * @return JSON data for this entry
   * @throws IOException if unable to read files from disk
   * @throws LookupException if entry is expired or corrupted
   */
  private static JSONObject getValidStoredData(Path entryDir, String computedDigest) throws IOException, LookupException {

    // 1. check digest against existing value
    if (!digestsMatch(entryDir, computedDigest)) {
      throw LookupException.EXPIRED_ENTRY;
    }

    // 2. check for presence of cached data, then read
    return readCachedData(entryDir)
        .orElseThrow(() -> LookupException.CORRUPTED_ENTRY);
  }

  /**
   * Checks if contents of digest file in the passed entry dir match a passed
   * computed digest; returns false if file is missing or digests don't match, else true\
   *
   * @param entryDir entry directory
   * @param computedDigest digest to which existing digest should be compared
   * @return whether digests match
   * @throws IOException if unable to read file
   */
  private static boolean digestsMatch(Path entryDir, String computedDigest) throws IOException {
    Path digestFile = entryDir.resolve(CACHE_DIGEST_FILE);
    return Files.exists(digestFile) &&
        Files.readString(digestFile).equals(computedDigest);
  }

  /**
   * Read cached data file from entry, returns empty optional if data file
   * does not exist or is unable to read or parsed into JSON.
   *
   * @param entryDir entry directory
   * @return optional entry data
   */
  private static Optional<JSONObject> readCachedData(Path entryDir) {
    try {
      Path file = entryDir.resolve(CACHED_DATA_FILE);
      return Files.exists(file)
        ? Optional.of(new JSONObject(Files.readString(file)))
        : Optional.empty();
    }
    catch (IOException | JSONException e) {
      LOG.error("Unable to read or parse cached data", e);
      return Optional.empty();
    }
  }

  /**
   * Returns a cached gene expression summary, generating and storing a new value if none
   * exists or if the existing value is out of date with the passed digests.
   *
   * @param summaryInputs gene summary inputs
   * @param experimentDescriber function to describe an experiment
   * @param experimentSummarizer function to summarize experiments into an expression summary
   * @return expression summary (will always be a cache hit)
   */
  public JSONObject populateSummary(GeneSummaryInputs summaryInputs,
      FunctionWithException<ExperimentInputs, CompletableFuture<JSONObject>> experimentDescriber,
      FunctionWithException<List<JSONObject>, JSONObject> experimentSummarizer) {
    try {
      return _cache.populateAndProcessContent(summaryInputs.getGeneId(),

          // populator
          entryDir -> {
            // first populate each dataset entry as needed and collect experiment descriptors
            List<JSONObject> experiments = populateExperiments(summaryInputs.getExperimentsWithData(), experimentDescriber);

            // summarize experiments and store
            getPopulator(summaryInputs.getDigest(), () -> experimentSummarizer.apply(experiments)).accept(entryDir);
          },

          // visitor
          entryDir -> getValidSummary(entryDir, summaryInputs),

          // repopulation predicate
          exceptionToTrue(entryDir ->
              // try to look up summary json; if cache miss, then repopulate
              getValidSummary(entryDir, summaryInputs).getString(CACHE_STATUS).equals(CACHE_MISS)));
    }
    catch (Exception e) {
      // any other exception is a 500
      throw e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e);
    }
  }

  /**
   * Returns a set of cached experiment descriptions, generating and storing new values for any
   * experiments not present or that are out of date (mismatched digests).  In this way, any new
   * experiments do not result in regeneration of descriptors for previously released experiments.
   *
   * @param experimentData experiment inputs
   * @param experimentDescriber function to describe an experiment
   * @return list of cached experiment descriptions
   * @throws Exception if unable to generate descriptions or store
   */
  private List<JSONObject> populateExperiments(List<ExperimentInputs> experimentData,
      FunctionWithException<ExperimentInputs, CompletableFuture<JSONObject>> experimentDescriber) throws Exception {
    List<JSONObject> experiments = new ArrayList<>();
    // start with serial generation; move back to parallel later
    for (ExperimentInputs input : experimentData) {
      experiments.add(_cache.populateAndProcessContent(input.getCacheKey(),

          // populator
          getPopulator(input.getDigest(), () -> experimentDescriber.apply(input).get()),

          // visitor
          experimentDir -> getValidStoredData(experimentDir, input.getDigest()),

          // repopulation predicate
          exceptionToTrue(experimentDir -> {
              getValidStoredData(experimentDir, input.getDigest());
              return false; // do not repopulate if able to look up valid value
          })
      ));
    }
    return experiments;
  }

  /**
   * Takes a predicate that throws an exception and returns a predicate that
   * does not, converting any thrown exception to true
   *
   * @param predicate predicate that throws an exception
   * @return the value returned by the passed predicate, or true if an exception is thrown
   */
  private Predicate<Path> exceptionToTrue(PredicateWithException<Path> predicate) {
    return path -> {
      try {
        return predicate.test(path);
      }
      catch (Exception e) {
        return true;
      }
    };
  }

  /**
   * Returns a function that populates a cache entry with the passed
   * digest and with data supplied by the passed supplier.
   *
   * @param digest digest to store
   * @param dataSupplier supplier of data to store
   * @return population function
   */
  private ConsumerWithException<Path> getPopulator(String digest, SupplierWithException<JSONObject> dataSupplier) {
    return entryDir -> {

      // write digest to digest file
      Files.writeString(entryDir.resolve(CACHE_DIGEST_FILE), digest);

      // write data
      Files.writeString(entryDir.resolve(CACHED_DATA_FILE), dataSupplier.get().toString());
    };
  }

}
