package org.apidb.apicommon.controller;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.Timer;
import org.gusdb.fgputil.functional.FunctionalInterfaces.ConsumerWithException;
import org.gusdb.fgputil.functional.FunctionalInterfaces.SupplierWithException;
import org.gusdb.fgputil.runtime.GusHome;
import org.gusdb.fgputil.runtime.RuntimeUtil;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkRuntimeException;

/**
 * Provides the ability to cache large data in a single file located in the WDK
 * tmp directory and delete that file.  This is useful for when a single,
 * config-dependent store of data is needed for the entire webapp- expensive to
 * create but frequently needed and too large to fit in memory.  It can be
 * generated either on startup or lazily, but does not change over the course of
 * the webapp load.  A hook should be placed on startup to remove the previous
 * iteration of the file; otherwise, the old one (from the previous deployment)
 * will be used.
 * 
 * @author rdoherty
 */
public class SiteSpecificTmpFileCache {

  private static final Logger LOG = Logger.getLogger(SiteSpecificTmpFileCache.class);

  private static final Map<String,ReadWriteLock> ACCESS_LOCKS = new ConcurrentHashMap<>();

  /**
   * Contains all the known uses of this class; used by the site initializer to
   * purge caches on startup.
   */
  public enum CacheName {
    ALL_RECORDS_EXPANDED("_all-records-expanded.json"),
    CATEGORIES_ONTOLOGY("_categories-ontology.json");

    private final String _suffix;

    private CacheName(String suffix) {
      _suffix = suffix;
    }
  }

  private static final String SITE_VALUE_MODEL_PROP_KEY = "LEGACY_WEBAPP_BASE_URL";

  private static Path getFileLocation(WdkModel wdkModel, String fileNameSuffix) throws WdkModelException {
    String siteSpecificPrefix = wdkModel.getProperties().get(SITE_VALUE_MODEL_PROP_KEY);
    if (siteSpecificPrefix == null) {
      throw new WdkModelException("Unable to use site-specific tmp file cache. model.prop does not contain key '" + SITE_VALUE_MODEL_PROP_KEY + "'");
    }
    if (siteSpecificPrefix.startsWith("/")) {
      siteSpecificPrefix = siteSpecificPrefix.substring(1);
    }
    return Paths.get(
      wdkModel.getModelConfig().getWdkTempDir().toAbsolutePath().toString(),
      siteSpecificPrefix + fileNameSuffix
    );
  }

  public static ConsumerWithException<OutputStream> get(WdkModel wdkModel, CacheName cacheName, SupplierWithException<InputStream> dataSupplier) throws WdkModelException {
    Path fileLocation = getFileLocation(wdkModel, cacheName._suffix);
    return out -> {
      try {

        // key for lock map is the absolute path to the cache file
        String lockMapKey = fileLocation.toAbsolutePath().toString();

        // add a lock for this path if not present
        ReadWriteLock lock = ACCESS_LOCKS.computeIfAbsent(lockMapKey, str -> new ReentrantReadWriteLock());

        // try to stream cached file; if successful, return
        if (streamCachedData(lock.readLock(), fileLocation, out, false)) return;

        // file not present; write it
        writeCachedData(lock.writeLock(), fileLocation, dataSupplier);

        // try again to stream; if not present this time, clear was called and OK to throw
        if (!streamCachedData(lock.readLock(), fileLocation, out, true))
          throw new WdkModelException("Expected cached file after generation but not " +
              "present (concurrent clear()) for file " + fileLocation.toAbsolutePath());
      }
      catch (Exception e) {
        throw new WdkModelException("Could not write to site-specific tmp file cache " + fileLocation.toAbsolutePath(), e);
      }
    };
  }

  private static void writeCachedData(Lock writeLock, Path fileLocation, SupplierWithException<InputStream> dataSupplier) throws Exception {
    writeLock.lock();
    try {
      // check again for file; maybe it was written while waiting for write lock
      File file = fileLocation.toFile();
      if (!file.exists()) {
        LOG.info("Could not find cache file " + fileLocation + ". Generating...");
        try (OutputStream cacheOutput = new BufferedOutputStream(new FileOutputStream(file))) {
          IoUtil.transferStream(cacheOutput , dataSupplier.get());
        }
      }
    }
    catch (Exception e) {
      // if exception is thrown during file generation, assume file is corrupted and remove
      Files.deleteIfExists(fileLocation);
      throw e;
    }
    finally {
      writeLock.unlock();
    }
  }

  // return true if successfully stream file, else false
  private static boolean streamCachedData(Lock readLock, Path fileLocation, OutputStream out, boolean freshlyGenerated) throws WdkModelException, IOException {
    readLock.lock();
    try {
      // check if file is present and return stream if so
      File file = fileLocation.toFile();
      if (file.exists()) {
        if (!file.isFile() || !file.canRead()) {
          throw new WdkModelException("Unable to use site-specific tmp file cache. " +  file.getAbsolutePath() + " exists but is not a readable file");
        }
        LOG.info(freshlyGenerated
            ? "Returning InputStream to newly generated cache file: " + fileLocation
            : "Returning InputStream to existing cache file: " + fileLocation
        );
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
          IoUtil.transferStream(out, in);
          return true;
        }
      }
      return false;
    }
    finally {
      readLock.unlock();
    }
  }

  public static void clearAll(WdkModel wdkModel) {
    for (CacheName cache : CacheName.values()) {
      clear(wdkModel, cache);
    }
  }

  public static void clear(WdkModel wdkModel, CacheName cacheName) {
    Path file = null;
    try {
      file = getFileLocation(wdkModel, cacheName._suffix);
      String lockMapKey = file.toAbsolutePath().toString();
      ReadWriteLock lock = ACCESS_LOCKS.computeIfAbsent(lockMapKey, str -> new ReentrantReadWriteLock());
      Lock writeLock = lock.writeLock();
      writeLock.lock();
      try {
        LOG.info("Deleting if exists: " + file);
        Files.deleteIfExists(file);
      }
      finally {
        writeLock.unlock();
      }
    }
    catch (IOException | WdkModelException e) {
      throw new WdkRuntimeException("Unable to delete site-specific tmp file cache " + file.toAbsolutePath(), e);
    }
  }

  public static void put(WdkModel wdkModel, CacheName cacheName, SupplierWithException<InputStream> dataSupplier, boolean useSubprocess, Class<?> cliClass) {
    try {
      Timer t = new Timer();
      LOG.info("Caching " + cacheName.name() + " (subprocess=" + useSubprocess + ")...");
      if (useSubprocess) {
        // In webapps, gus_home is of the form: /var/www/PlasmoDB/plasmo.rdoherty/webapp/WEB-INF/wdk-model/
        // But there is no /java soft link under lib there; instead, use the "real" gus_home directory under
        // the webapps dir.
        String gusHome = Paths.get(GusHome.getGusHome())
            .getParent().getParent().getParent().resolve("gus_home").toString();
        LOG.info("Using GUS_HOME = " + gusHome);
        RuntimeUtil.executeSubprocess(
            List.of("perl", gusHome + "/bin/fgpJava", cliClass.getName(), wdkModel.getProjectId()),
            Map.of("GUS_HOME", gusHome),               // subprocess environment 
            Optional.empty(),                          // don't override stdin
            s -> LOG.info(">> " + s),                  // log both stdout/stderr
            Optional.empty(),                          // don't send stdout to a file
            Optional.of(Duration.ofMinutes(1)));       // use timeout
      }
      else {
        // write the cached file
        put(wdkModel, cacheName, dataSupplier);
      }
      LOG.info("Caching complete; took " + t.getElapsedString());
    }
    catch (Exception e) {
      String message = "Could not cache " + cacheName.name() + " for file streaming.";
      LOG.error(message, e);
      throw new WdkRuntimeException(message, e);
    }
  }

  private static void put(WdkModel wdkModel, CacheName cacheName, SupplierWithException<InputStream> dataSupplier) throws WdkModelException {
    Path fileLocation = getFileLocation(wdkModel, cacheName._suffix);
    String lockMapKey = fileLocation.toAbsolutePath().toString();
    ReadWriteLock lock = ACCESS_LOCKS.computeIfAbsent(lockMapKey, str -> new ReentrantReadWriteLock());
    try {
      writeCachedData(lock.writeLock(), fileLocation, dataSupplier);
    }
    catch (Exception e) {
      if (e instanceof RuntimeException) throw (RuntimeException)e;
      if (e instanceof WdkModelException) throw (WdkModelException)e;
      throw new WdkModelException("Unable to write cache file to " + fileLocation, e);
    }
  }
}
