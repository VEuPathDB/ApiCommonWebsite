package org.apidb.apicommon.model.report.ai.expression;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;

import org.apidb.apicommon.model.report.ai.expression.GeneRecordProcessor.GeneSummaryInputs;
import org.gusdb.fgputil.cache.disk.OnDiskCache;
import org.gusdb.fgputil.cache.disk.OnDiskCache.EntryNotCreatedException;
import org.gusdb.fgputil.cache.disk.OnDiskCache.Overwrite;
import org.gusdb.fgputil.functional.FunctionalInterfaces.ConsumerWithException;
import org.gusdb.fgputil.functional.FunctionalInterfaces.FunctionWithException;
import org.gusdb.wdk.model.WdkModel;
import org.json.JSONException;
import org.json.JSONObject;

public class AiExpressionCache {

  // constants to determine cache location
  private static final String CACHE_DIR_PROP_NAME = "AI_EXPRESSION_CACHE_DIR";
  private static final String DEFAULT_TMP_CACHE_SUBDIR = "expressionCache";

  private static final long DEFAULT_TIMEOUT_MILLIS = 5000;
  private static final long DEFAULT_POLL_FREQUENCY_MILLIS = 500;

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

  private final WdkModel _wdkModel;
  private final OnDiskCache _cache;

  public AiExpressionCache(WdkModel wdkModel) throws IOException {
    _wdkModel = wdkModel;

    Path cacheParentDir = Optional
        .ofNullable(_wdkModel.getProperties().get(CACHE_DIR_PROP_NAME))
        .map(Paths::get)
        .orElse(Paths.get(_wdkModel.getModelConfig().getWdkTempDir().toString(), DEFAULT_TMP_CACHE_SUBDIR));

    _cache = new OnDiskCache(cacheParentDir, DEFAULT_TIMEOUT_MILLIS, DEFAULT_POLL_FREQUENCY_MILLIS);

  }

  public void blah() {
    _cache.populateAndProcessContent(geneId, populator, visitor, overwritePredicate)
  }

  // Check if cached data is valid
  public boolean isCacheValid(GeneSummaryInputs summaryInputs) {
    try {
      FunctionWithException<Path, Boolean> visitor = entryDir -> {
        Path digestFile = entryDir.resolve("digest.txt");

        if (!Files.exists(digestFile)) {
          System.out.println("No digest file found.");
          return false;
        }

        // Read stored digest and compare
        String cachedDigest = Files.readString(digestFile);

        if (cachedDigest.equals(summaryInputs.getExperimentsDigest())) {
          System.out.println("Cache digest matches input.");
          return true;
        }
        else {
          System.out.println("Cache digest mismatch! Cache is out of date.");
          return false;
        }
      };

      return _cache.visitContent(summaryInputs.getGeneId(), visitor);

    }
    catch (EntryNotCreatedException e) {
      System.out.println("Cache entry does not exist yet.");
      return false;
    }
    catch (Exception e) {
      throw new RuntimeException("Error validating cache entry", e);
    }
  }

  // Populate cache with computed data (Method 1: Takes computedData directly)
  public void populateCache(GeneSummaryInputs summaryInputs, JSONObject computedData) throws Exception {
    ConsumerWithException<Path> populator = entryDir -> {
      Files.writeString(entryDir.resolve("cached_data.txt"), computedData.toString());
      Files.writeString(entryDir.resolve("digest.txt"), summaryInputs.getExperimentsDigest());
    };

    // Populate with overwrite policy (assumes caller ensures it's necessary)
    _cache.populateAndProcessContent(summaryInputs.getGeneId(), populator, path -> null, Overwrite.YES);
  }

//  // Populate cache with a function that computes the result (Method 2: Uses a function)
//  public void populateCache(String cacheKey, String inputData, Function<String, String> computation) throws Exception {
//    populateCache(cacheKey, inputData, computation.apply(inputData));
//  }


  // Read cached data (throws IOException if missing)
  public JSONObject readCachedData(String cacheKey) throws Exception {
    FunctionWithException<Path, JSONObject> visitor = entryDir -> {
      Path file = entryDir.resolve("cached_data.txt");
      if (!Files.exists(file)) {
	throw new IOException("Cache entry missing: " + file);
      }
      String fileContents = Files.readString(file);
      try {
	JSONObject jsonObject = new JSONObject(fileContents);
	return jsonObject;
      } catch (JSONException e) {
	throw e;
      }
    };
    
    return _cache.visitContent(cacheKey, visitor);
  }

}
