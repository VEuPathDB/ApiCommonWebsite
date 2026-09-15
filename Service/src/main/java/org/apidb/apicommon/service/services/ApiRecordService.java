package org.apidb.apicommon.service.services;

import java.io.OutputStream;

import org.apache.log4j.Logger;
import org.apidb.apicommon.controller.SiteSpecificTmpFileCache;
import org.apidb.apicommon.controller.SiteSpecificTmpFileCache.CacheName;
import org.gusdb.fgputil.functional.FunctionalInterfaces.ConsumerWithException;
import org.gusdb.fgputil.runtime.GusHome;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.service.service.RecordService;

public class ApiRecordService extends RecordService {

  private static final Logger LOG = Logger.getLogger(ApiRecordService.class);

  @Override
  protected ConsumerWithException<OutputStream> getExpandedRecordClassesJsonStreamer(WdkModel wdkModel) throws WdkModelException {
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
      return super.getExpandedRecordClassesJsonStreamer(wdkModel);
    }

    // otherwise use cache mechanism for efficient delivery of expanded records json
    return SiteSpecificTmpFileCache.get(wdkModel, CacheName.ALL_RECORDS_EXPANDED, () -> getExpandedRecordClassesJson(wdkModel));
  }

  public static void cacheExpandedRecordClassesJson(WdkModel wdkModel, boolean useSubprocess) {
    SiteSpecificTmpFileCache.put(wdkModel, CacheName.ALL_RECORDS_EXPANDED,
        () -> getExpandedRecordClassesJson(wdkModel), useSubprocess, ApiRecordService.class);
  }

  public static void main(String[] args) throws WdkModelException {
    if (args.length != 1)
      throw new IllegalArgumentException("This tool requires a single argument, project_id");
    try (WdkModel wdkModel = WdkModel.construct(args[0], GusHome.getGusHome())) {
      cacheExpandedRecordClassesJson(wdkModel, false);
    }
  }
}
