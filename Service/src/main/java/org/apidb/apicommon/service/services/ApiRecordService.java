package org.apidb.apicommon.service.services;

import static org.gusdb.fgputil.functional.Functions.f0Swallow;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apidb.apicommon.controller.SiteSpecificTmpFileCache;
import org.apidb.apicommon.controller.SiteSpecificTmpFileCache.CacheName;
import org.eupathdb.common.controller.WebsiteReleaseConstants;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.Timer;
import org.gusdb.fgputil.runtime.GusHome;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.service.service.RecordService;

public class ApiRecordService extends RecordService {

  private static final Logger LOG = Logger.getLogger(ApiRecordService.class);

  @Override
  protected InputStream getExpandedRecordClassesJsonStream(WdkModel wdkModel) {
    try {
      if (_servletRequest == null
          || _servletRequest.getAttribute("WEBSITE_RELEASE_STAGE") == null
          || !FormatUtil.isInteger((String)_servletRequest.getAttribute("WEBSITE_RELEASE_STAGE"))
          || Integer.parseInt((String)_servletRequest.getAttribute("WEBSITE_RELEASE_STAGE")) <= WebsiteReleaseConstants.DEVELOPMENT
      ) {
        // if Grizzly, or stage not forwarded, or stage not an int, or stage == development, do not cache
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
      LOG.info("Caching expanded record classes JSON (subprocess=" + useSubprocess + "...");
      if (useSubprocess) {
        Process process = new ProcessBuilder()
            .command(List.of("fgpJava", ApiRecordService.class.getName(), wdkModel.getProjectId()))
            .inheritIO()
            .start();
        if (process.waitFor(2, TimeUnit.MINUTES)) {
          if (process.exitValue() != 0) {
            throw new WdkModelException("Subprocess exited with error code " + process.exitValue());
          }
        }
        else {
          throw new WdkModelException("Subprocess timed out before completion");
        }
      }
      else {
        ApiRecordService obj = new ApiRecordService();
        try (InputStream in = obj.getCachedExpandedRecordClassesJson(wdkModel)) {
          // nothing to do; just close the stream
        }
      }
      LOG.info("Caching complete; took " + t.getElapsedString());
    }
    catch (IOException | WdkModelException | InterruptedException e) {
      LOG.error("Could not cache expanded record classes JSON for file streaming.  This error is being ignored.", e);
    }
  }

  public static void main(String[] args) throws WdkModelException {
    if (args.length != 1)
      throw new IllegalArgumentException("This tool requires a single argument, project_id");
    try (WdkModel wdkModel = WdkModel.construct(args[0], GusHome.getGusHome())) {
      cacheExpandedRecordClassesJson(wdkModel, false);
    }
  }
}
