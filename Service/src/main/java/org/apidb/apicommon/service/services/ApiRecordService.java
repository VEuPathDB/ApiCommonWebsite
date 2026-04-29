package org.apidb.apicommon.service.services;

import static org.gusdb.fgputil.functional.Functions.f0Swallow;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.apidb.apicommon.controller.SiteSpecificTmpFileCache;
import org.apidb.apicommon.controller.SiteSpecificTmpFileCache.CacheName;
import org.gusdb.fgputil.Timer;
import org.gusdb.fgputil.runtime.GusHome;
import org.gusdb.fgputil.runtime.RuntimeUtil;
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
        // But there is no /java soft link under lib there; instead, use the "real" gus_home directory under
        // the webapps dir.
        String gusHome = Paths.get(GusHome.getGusHome())
            .getParent().getParent().getParent().resolve("gus_home").toString();
        LOG.info("Using GUS_HOME = " + gusHome);
        RuntimeUtil.executeSubprocess(
            List.of("perl", gusHome + "/bin/fgpJava", ApiRecordService.class.getName(), wdkModel.getProjectId()),
            Map.of("GUS_HOME", gusHome),               // subprocess environment 
            Optional.empty(),                          // don't override stdin
            s -> LOG.info(">> " + s),                  // log both stdout/stderr
            Optional.empty(),                          // don't send stdout to a file
            Optional.of(Duration.ofMinutes(1)));       // use timeout
      }
      else {
        ApiRecordService obj = new ApiRecordService();
        try (InputStream in = obj.getCachedExpandedRecordClassesJson(wdkModel)) {
          // nothing to do but close the stream
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
}
