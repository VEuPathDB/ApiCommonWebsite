package org.apidb.apicommon.model.report.ai.expression;

import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.gusdb.fgputil.functional.Functions.wrapException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

import org.apache.log4j.Logger;
import org.apidb.apicommon.model.report.ai.expression.GeneRecordProcessor.ExperimentInputs;
import org.apidb.apicommon.model.report.ai.expression.GeneRecordProcessor.GeneSummaryInputs;
import org.gusdb.fgputil.cache.disk.DirectoryLock.DirectoryLockTimeoutException;
import org.gusdb.fgputil.cache.disk.OnDiskCache;
import org.gusdb.fgputil.cache.disk.OnDiskCache.EntryNotCreatedException;
import org.gusdb.fgputil.functional.Either;
import org.gusdb.fgputil.functional.FunctionalInterfaces.ConsumerWithException;
import org.gusdb.fgputil.functional.FunctionalInterfaces.FunctionWithException;
import org.gusdb.fgputil.functional.FunctionalInterfaces.PredicateWithException;
import org.gusdb.fgputil.functional.FunctionalInterfaces.SupplierWithException;
import org.gusdb.wdk.model.WdkModel;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Provides lookup methods for gene expression summaries, caching by gene ID,
 * with and without the option of cache population.
 *
 * Summary success response will look like:
 * {
 *   "resultStatus": "present",
 *   "expressionSummary": generated_summary_object
 * }
 *
 * Summary failure response will look like:
 * {
 *   "resultStatus": StatusString (not "present"),
 *   "numExperiments": 25,
 *   "numExperimentsComplete": 10,
 *   "experimentStatus": {
 *     [datasetId]: StatusString
 *   }
 * }
 *
 * Where StatusString is one of:
 * - "present"
 * - "missing"
 * - "failed"
 * - "expired"
 * - "corrupted"
 * - "undetermined"
 * - "experiments_incomplete" (summary only)
 *
 * @param summaryInputs inputs for cache lookup
 * @return response JSON (indicating cache hit or not with data or miss reason respectively)
 */
public class AiExpressionCache {

  private static Logger LOG = Logger.getLogger(AiExpressionCache.class);

  // parallel processing
  private static final int MAX_CONCURRENT_EXPERIMENT_LOOKUPS_PER_REQUEST = 10;
  private static final long VISIT_ENTRY_LOCK_MAX_WAIT_MILLIS = 50;

  // cache location
  private static final String CACHE_DIR_PROP_NAME = "AI_EXPRESSION_CACHE_DIR";
  private static final String DEFAULT_TMP_CACHE_SUBDIR = "expressionCache";

  // catch characteristics
  private static final long DEFAULT_TIMEOUT_MILLIS = 5 * 60 * 1000;
  private static final long DEFAULT_POLL_FREQUENCY_MILLIS = 500;

  // cache filenames
  private static final String CACHED_DATA_FILE = "cached_data.txt";
  private static final String CACHE_DIGEST_FILE = "digest.txt";

  // returned JSON props
  private static final String RESULT_STATUS_PROP = "resultStatus";
  private static final String SUMMARY_RESULT_PROP = "expressionSummary";
  private static final String NUM_EXPERIMENTS_PROP = "numExperiments";
  private static final String NUM_EXPERIMENTS_COMPLETE_PROP = "numExperimentsComplete";
  private static final String EXPERIMENT_STATUS_PROP = "experimentStatus";

  // status messages
  private static enum Status {
    PRESENT,
    MISSING,
    FAILED,
    EXPIRED,
    CORRUPTED,
    UNDETERMINED,
    EXPERIMENTS_INCOMPLETE;

    String val() {
      return name().toLowerCase();
    }
  }

  private static class LookupException extends Exception {
    public LookupException(Status status) {
      super(status.name());
    }
    public Status getStatus() {
      return Status.valueOf(getMessage());
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

  public JSONObject readSummary(GeneSummaryInputs summaryInputs) {

    // collect status of each experiment
    int numComplete = 0;
    Map<String, String> experiments = new LinkedHashMap<>();
    for (ExperimentInputs datasetInput : summaryInputs.getExperimentsWithData()) {
      Status status = getEntryStatus(datasetInput.getCacheKey(), datasetInput.getDigest());
      if (status == Status.PRESENT) {
        numComplete++;
      }
      experiments.put(datasetInput.getDatasetId(), status.val());
    }

    if (numComplete == summaryInputs.getExperimentsWithData().size()) {
      // all experiments complete; check summary using short wait time
      Status status = getEntryStatus(summaryInputs.getGeneId(), summaryInputs.getDigest());
      if (status == Status.PRESENT) {
        // all done; read result
        try {
          JSONObject summary = _cache.visitContent(summaryInputs.getGeneId(), dir -> getValidStoredData(dir, summaryInputs.getDigest()));
          return new JSONObject()
              .put(RESULT_STATUS_PROP, Status.PRESENT.val())
              .put(SUMMARY_RESULT_PROP, summary);
        }
        catch (Exception e) {
          // would not expect this since we already checked and are waiting for lock
          throw new RuntimeException("Unable to read summary after already checking validity", e);
        }
      }
      else {
        return new JSONObject()
            .put(RESULT_STATUS_PROP, status.val())
            .put(NUM_EXPERIMENTS_PROP, summaryInputs.getExperimentsWithData().size())
            .put(NUM_EXPERIMENTS_COMPLETE_PROP, numComplete)
            .put(EXPERIMENT_STATUS_PROP, new JSONObject(experiments));
      }
    }
    else {
      // experiments incomplete; return appropriate response
      return new JSONObject()
          .put(RESULT_STATUS_PROP, Status.EXPERIMENTS_INCOMPLETE.val())
          .put(NUM_EXPERIMENTS_PROP, summaryInputs.getExperimentsWithData().size())
          .put(NUM_EXPERIMENTS_COMPLETE_PROP, numComplete)
          .put(EXPERIMENT_STATUS_PROP, new JSONObject(experiments));
    }
  }

  private Status getEntryStatus(String cacheKey, String digest) {
    try {
      // visit the summary to see if it is complete
      _cache.visitContent(cacheKey,
          dir -> getValidStoredData(dir, digest),
          VISIT_ENTRY_LOCK_MAX_WAIT_MILLIS);
      return Status.PRESENT;
    }
    catch (EntryNotCreatedException e) {
      return Status.MISSING;
    }
    catch (DirectoryLockTimeoutException e) {
      return Status.UNDETERMINED;
    }
    catch (LookupException e) {
      return e.getStatus();
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
    // if any are missing or expired, exception will be thrown indicating a cache miss
    for (ExperimentInputs datasetInput : summaryInputs.getExperimentsWithData()) {
      _cache.visitContent(datasetInput.getCacheKey(), experimentDir -> {
        return getValidStoredData(experimentDir, datasetInput.getDigest());
      });
    }

    // once all experiment values are confirmed, check for valid summary entry
    JSONObject summary = getValidStoredData(geneDir, summaryInputs.getDigest());
    return new JSONObject()
        .put(RESULT_STATUS_PROP, Status.PRESENT.val())
        .put(SUMMARY_RESULT_PROP, summary);
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

    // 1. check if entry is complete / not failed
    if (!OnDiskCache.isEntryComplete(entryDir)) {
      throw new LookupException(Status.FAILED);
    }

    // 2. check digest against existing value
    if (!digestsMatch(entryDir, computedDigest)) {
      throw new LookupException(Status.EXPIRED);
    }

    // 3. check for presence of cached data, then read
    return readCachedData(entryDir)
        .orElseThrow(() -> new LookupException(Status.CORRUPTED));
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

            // sort them most-interesting first so that the "Other" section will be filled
            // in that order (and also to give the AI the data in a sensible order)
            experiments.sort(Comparator
                .comparing((JSONObject obj) -> obj.optInt("biological_importance"), Comparator.reverseOrder())
                .thenComparing(obj -> obj.optInt("confidence"), Comparator.reverseOrder())
            );

            // summarize experiments and store
            getPopulator(summaryInputs.getDigest(), () -> experimentSummarizer.apply(experiments)).accept(entryDir);
          },

          // visitor
          entryDir -> getValidSummary(entryDir, summaryInputs),

          // repopulation predicate
          exceptionToTrue(entryDir ->
              // try to look up summary json; if not present, then try to repopulate
              !getValidSummary(entryDir, summaryInputs).getString(RESULT_STATUS_PROP).equals(Status.PRESENT.val())));
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

    // use a thread for each experiment, up to a reasonable max
    int threadPoolSize = Math.min(MAX_CONCURRENT_EXPERIMENT_LOOKUPS_PER_REQUEST, experimentData.size());

    ExecutorService exec = Executors.newFixedThreadPool(threadPoolSize);
    try {
      // look up experiment results in parallel, wait for completion, and aggregate results
      List<CompletableFuture<JSONObject>> results = new ArrayList<>();
      for (ExperimentInputs input : experimentData) {

        results.add(supplyAsync(() -> wrapException(() -> _cache.populateAndProcessContent(input.getCacheKey(),

          // populator
          getPopulator(input.getDigest(), () -> experimentDescriber.apply(input).get()),

          // visitor
          experimentDir -> getValidStoredData(experimentDir, input.getDigest()),

          // repopulation predicate
          exceptionToTrue(experimentDir -> {
            getValidStoredData(experimentDir, input.getDigest());
            return false; // do not repopulate if able to look up valid value
          }))

        ), exec));
      }

      // wait for all threads, filling lists along the way
      List<JSONObject> descriptors = new ArrayList<>();
      List<Throwable> exceptions = new ArrayList<>();
      for (CompletableFuture<JSONObject> result : results) {
        result.handle(Either::new).get().ifLeft(descriptors::add).ifRight(exceptions::add);
      }

      // if no exceptions occurred, return results; else throw first problem
      if (exceptions.isEmpty()) {
        return descriptors;
      }
      throw new RuntimeException(exceptions.get(0));
    }
    finally {
      exec.shutdown();
    }
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
