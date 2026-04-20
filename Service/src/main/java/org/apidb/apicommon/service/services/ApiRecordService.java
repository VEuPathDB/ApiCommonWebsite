package org.apidb.apicommon.service.services;

import static org.gusdb.fgputil.functional.Functions.f0Swallow;
import static org.gusdb.fgputil.functional.Functions.swallowAndGet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apidb.apicommon.controller.SiteSpecificTmpFileCache;
import org.apidb.apicommon.controller.SiteSpecificTmpFileCache.CacheName;
import org.gusdb.fgputil.Timer;
import org.gusdb.fgputil.runtime.GusHome;
import org.gusdb.fgputil.runtime.ThreadUtil;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.gusdb.wdk.service.service.RecordService;

public class ApiRecordService extends RecordService {

  private static final Logger LOG = Logger.getLogger(ApiRecordService.class);

  @Override
  protected InputStream getExpandedRecordClassesJsonStream(WdkModel wdkModel) {
    try {
      if (_servletRequest == null
          // Uncommenting the following code will turn off caching for development sites,
          //   which proved to be problematic, causing an increase in OOMs as storing the JSON
          //   in memory, even if only briefly, is expensive
          /*|| _servletRequest.getAttribute("WEBSITE_RELEASE_STAGE") == null
          || !FormatUtil.isInteger((String)_servletRequest.getAttribute("WEBSITE_RELEASE_STAGE"))
          || Integer.parseInt((String)_servletRequest.getAttribute("WEBSITE_RELEASE_STAGE")) <= WebsiteReleaseConstants.DEVELOPMENT*/
      ) {
        // if Grizzly, or stage not forwarded, or stage not an int, or stage == development, do not cache
        LOG.warn("Skipping cache for expanded recordclass JSON because " +
            (_servletRequest == null ? "servlet request is null" : "release stage is " + _servletRequest.getAttribute("WEBSITE_RELEASE_STAGE")));
        return super.getExpandedRecordClassesJsonStream(wdkModel);
      }
  
      // otherwise try to use cached version of this response for this site
      return getCachedExpandedRecordClassesJson(wdkModel);
    }
    catch (Exception e) {
      // don't let an exception prevent delivery of data to the client; log and trigger email
      LOG.error("Unable to cache expanded record class JSON data to file", e);
      triggerErrorEvents(Collections.singletonList(e));
      return super.getExpandedRecordClassesJsonStream(wdkModel);
    }
  }

  private InputStream getCachedExpandedRecordClassesJson(WdkModel wdkModel) throws WdkModelException {
    return SiteSpecificTmpFileCache.get(wdkModel, CacheName.ALL_RECORDS_EXPANDED,
        f0Swallow(() -> super.getExpandedRecordClassesJsonStream(wdkModel)));
  }

  public static void cacheExpandedRecordClassesJson(WdkModel wdkModel, boolean useSubprocess) {
    try {
      Timer t = new Timer();
      LOG.info("Caching expanded record classes JSON (subprocess=" + useSubprocess + ")...");
      if (useSubprocess) {
        // In webapps, gus_home is of the form: /var/www/PlasmoDB/plasmo.rdoherty/webapp/WEB-INF/wdk-model/
        // But there is no java soft link under lib there; instead, use the "real" gus_home directory under
        // the webapps dir.
        String gusHome = Paths.get(GusHome.getGusHome())
            .getParent().getParent().getParent().resolve("gus_home").toString();
        LOG.info("Using GUS_HOME = " + gusHome);
        executeAndLogOutput(
            List.of("perl", gusHome + "/bin/fgpJava", ApiRecordService.class.getName(), wdkModel.getProjectId()),
            Map.of("GUS_HOME", gusHome),
            LOG, Level.INFO, Optional.of(Duration.ofMinutes(1)), true);
      }
      else {
        ApiRecordService obj = new ApiRecordService();
        try (InputStream in = obj.getCachedExpandedRecordClassesJson(wdkModel)) {
          // nothing to do; just close the stream
        }
      }
      LOG.info("Caching complete; took " + t.getElapsedString());
    }
    catch (IOException | WdkModelException e) {
      String message = "Could not cache expanded record classes JSON for file streaming.";
      LOG.error(message, e);
      throw new WdkRuntimeException(message, e);
    }
  }

  public static void main(String[] args) throws WdkModelException {
    if (args.length != 1)
      throw new IllegalArgumentException("This tool requires a single argument, project_id");
    try (WdkModel wdkModel = WdkModel.construct(args[0], GusHome.getGusHome())) {
      cacheExpandedRecordClassesJson(wdkModel, false);
    }
  }

  // FIXME: This method also exists in FgpUtil's RuntimeUtil class; use as soon as it is available via upgrade
  public static void executeAndLogOutput(List<String> command, Map<String,String> environment,
      Logger logger, Level logLevel, Optional<Duration> processTimeout, boolean killOnTimeout) {
    Thread logMonitorThread = null;
    try {
      LOG.info("Starting subprocess with command: " + String.join(" ", command));
      // start the process
      ProcessBuilder processBuilder = new ProcessBuilder()
          .command(command)
          .redirectErrorStream(true);
      processBuilder.environment().putAll(environment);
      Process process = processBuilder.start();

      // start a thread to stream output to the passed Logger (if level allows)
      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      logMonitorThread = new Thread(() -> {
        try {
          String line;
          while ((line = reader.readLine()) != null) {
            logger.log(logLevel, ">> " + line);
          }
        }
        catch (IOException e) {
          LOG.error("Parent process warning: could not read subprocess output", e);
        }
      });
      logMonitorThread.start();

      // wait for process to finish in this thread
      boolean exitWithoutTimeout = processTimeout
          .map(duration -> swallowAndGet(() -> process.waitFor(duration.toMillis(), TimeUnit.MILLISECONDS)))
          .orElse(process.waitFor() - process.exitValue() == 0); // always true

      // wait for the thread to finish processing the subprocess's output
      logMonitorThread.join();

      if (exitWithoutTimeout) {
        LOG.info("Subprocess exited with exit code: " + process.exitValue());
        if (process.exitValue() != 0) {
          throw new RuntimeException("Subprocess exited with error code " + process.exitValue());
        }
      }
      else {
        // subprocess timed out before completion; kill if requested
        if (killOnTimeout) {
          int gracefulShutdownWindow = 500;
          LOG.info("Subprocess timed out before completion.  Attempting to shut down gracefully...");
          process.destroy();
          ThreadUtil.sleep(gracefulShutdownWindow);
          if (process.isAlive()) {
            LOG.info("Subprocess did not shut down gracefully after " + gracefulShutdownWindow + "ms.  Forcibly terminating.");
            process.destroyForcibly();
          }
        }
        throw new RuntimeException("Subprocess timed out before completion");
      }
    }
    catch (InterruptedException e) {
      throw new RuntimeException("Subprocess was interrupted before completion", e);
    }
    catch (IOException e) {
      throw new RuntimeException("Error occurred while executing subprocess", e);
    }
    finally {
      // kill log monitor thread if still active
      if (logMonitorThread != null && logMonitorThread.isAlive()) {
        logMonitorThread.interrupt();
      }
    }
  }
}
