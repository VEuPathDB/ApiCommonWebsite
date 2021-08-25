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
 * iteration of the file; otherwise, the old one (from the prervious deployment)
 * will be used.
 * 
 * @author rdoherty
 *
 */
public class SiteSpecificTmpFileCache {

  /**
   * Contains all the known uses of this class; used by the site initializer to
   * purge caches on startup.
   */
  public enum CacheNames {
    ALL_RECORDS_EXPANDED("_all-records-expanded.json");

    public final String SUFFIX;

    private CacheNames(String suffix) {
      SUFFIX = suffix;
    }
  }

  private static final String SITE_VALUE_MODEL_PROP_KEY = "LEGACY_WEBAPP_BASE_URL";

  public static InputStream get(WdkModel wdkModel, String fileNameSuffix, Supplier<InputStream> dataSupplier) throws WdkModelException {
    return getCachedData(getFileLocation(wdkModel, fileNameSuffix), dataSupplier);
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
        return new BufferedInputStream(new FileInputStream(file));
      }
      // file not present; write out
      try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
        IoUtil.transferStream(out , dataSupplier.get());
        return new BufferedInputStream(new FileInputStream(file));
      }
    }
    catch (IOException e) {
      throw new WdkModelException("Could not write to site-specific tmp file cache " + fileLocation.toAbsolutePath(), e);
    }
  }

  public static void clear(WdkModel wdkModel, String fileNameSuffix) {
    Path file = null;
    try {
      file = getFileLocation(wdkModel, fileNameSuffix);
      Files.deleteIfExists(file);
    }
    catch (IOException | WdkModelException e) {
      throw new WdkRuntimeException("Unable to delete site-specific tmp file cache " + file.toAbsolutePath(), e);
    }
  }

}
