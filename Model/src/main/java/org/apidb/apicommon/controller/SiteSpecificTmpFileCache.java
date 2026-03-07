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
import java.util.function.Supplier;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.IoUtil;
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

  /**
   * Contains all the known uses of this class; used by the site initializer to
   * purge caches on startup.
   */
  public enum CacheName {
    ALL_RECORDS_EXPANDED("_all-records-expanded.json");

    private final String _suffix;

    private CacheName(String suffix) {
      _suffix = suffix;
    }
  }

  private static final String SITE_VALUE_MODEL_PROP_KEY = "LEGACY_WEBAPP_BASE_URL";

  public static InputStream get(WdkModel wdkModel, CacheName cacheName, Supplier<InputStream> dataSupplier) throws WdkModelException {
    Path path = getFileLocation(wdkModel, cacheName._suffix);
    return getCachedData(path, dataSupplier);
  }

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

  private static synchronized InputStream getCachedData(Path fileLocation, Supplier<InputStream> dataSupplier) throws WdkModelException {
    try {
      File file = fileLocation.toFile();
      if (file.exists()) {
        if (!file.isFile() || !file.canRead()) {
          throw new WdkModelException("Unable to use site-specific tmp file cache. " +  file.getAbsolutePath() + " exists but is not a readable file");
        }
        LOG.info("Returning InputStream to existing cache file: " + fileLocation);
        return new BufferedInputStream(new FileInputStream(file));
      }
      // file not present; write out
      LOG.info("Could not find cache file " + fileLocation + ". Generating...");
      try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
        IoUtil.transferStream(out , dataSupplier.get());
      }

      LOG.info("Returning InputStream to newly generated cache file: " + fileLocation);
      return new BufferedInputStream(new FileInputStream(file));
    }
    catch (IOException e) {
      throw new WdkModelException("Could not write to site-specific tmp file cache " + fileLocation.toAbsolutePath(), e);
    }
  }

  public static void clear(WdkModel wdkModel, CacheName cacheName) {
    Path file = null;
    try {
      file = getFileLocation(wdkModel, cacheName._suffix);
      LOG.info("Deleting if exists: " + file);
      Files.deleteIfExists(file);
    }
    catch (IOException | WdkModelException e) {
      throw new WdkRuntimeException("Unable to delete site-specific tmp file cache " + file.toAbsolutePath(), e);
    }
  }

}
