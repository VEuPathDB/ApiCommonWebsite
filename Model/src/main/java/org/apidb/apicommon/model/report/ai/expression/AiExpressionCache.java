package org.apidb.apicommon.model.report.ai.expression;


import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.Function;
import org.json.JSONObject;
import org.json.JSONException;

import org.gusdb.fgputil.cache.disk.OnDiskCache;
import org.gusdb.fgputil.functional.FunctionalInterfaces.ConsumerWithException;
import org.gusdb.fgputil.functional.FunctionalInterfaces.FunctionWithException;

public class AiExpressionCache extends OnDiskCache {

  // Default cache location and timing settings
  private static final Path DEFAULT_CACHE_DIR = Paths.get("/tmp/expressionCache");
  private static final long DEFAULT_TIMEOUT_MILLIS = 5000;
  private static final long DEFAULT_POLL_FREQUENCY_MILLIS = 500;

  // No-argument constructor using defaults
  public AiExpressionCache() throws IOException {
    super(DEFAULT_CACHE_DIR, DEFAULT_TIMEOUT_MILLIS, DEFAULT_POLL_FREQUENCY_MILLIS);
  }

  // Compute SHA-256 hash digest of input
  private static String computeDigest(String input) throws NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(input.getBytes());
    return HexFormat.of().formatHex(hash);
  }

  // Check if cached data is valid
  public boolean isCacheValid(String cacheKey, String inputData) {

    try {
      FunctionWithException<Path, Boolean> visitor = entryDir -> {
	  Path digestFile = entryDir.resolve("digest.txt");

	  if (!Files.exists(digestFile)) {
	    System.out.println("No digest file found.");
	    return false;
	  }

	  // Read stored digest and compare
	  String cachedDigest = Files.readString(digestFile);
	  String computedDigest = computeDigest(inputData);

	  if (cachedDigest.equals(computedDigest)) {
	    System.out.println("Cache digest matches input.");
	    return true;
	  } else {
	    System.out.println("Cache digest mismatch! Cache is out of date.");
	    return false;
	  }
      };

      return visitContent(cacheKey, visitor);

    } catch (EntryNotCreatedException e) {
      System.out.println("Cache entry does not exist yet.");
      return false;
    } catch (Exception e) {
      throw new RuntimeException("Error validating cache entry", e);
    }
  }

  // Populate cache with computed data (Method 1: Takes computedData directly)
  public void populateCache(String cacheKey, String inputData, JSONObject computedData) throws Exception {
    ConsumerWithException<Path> populator = entryDir -> {
      Files.writeString(entryDir.resolve("cached_data.txt"), computedData.toString());
      Files.writeString(entryDir.resolve("digest.txt"), computeDigest(inputData));
    };

    // Populate with overwrite policy (assumes caller ensures it's necessary)
    populateAndProcessContent(cacheKey, populator, path -> null, Overwrite.YES);
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
    
    return visitContent(cacheKey, visitor);
  }

}
