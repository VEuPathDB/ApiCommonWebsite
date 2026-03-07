package org.apidb.apicommon.service;

import org.apidb.apicommon.controller.ApiSiteEventHandlers;
import org.apidb.apicommon.controller.CommentFactoryManager;
import org.apidb.apicommon.controller.SiteSpecificTmpFileCache;
import org.apidb.apicommon.controller.SiteSpecificTmpFileCache.CacheNames;
import org.apidb.apicommon.model.DataPlotterQueries;
import org.apidb.apicommon.model.JBrowseQueries;
import org.eupathdb.common.controller.EuPathSiteSetup;
import org.gusdb.fgputil.web.ApplicationContext;
import org.gusdb.wdk.controller.WdkInitializer;
import org.gusdb.wdk.model.WdkModel;

public class ApiSiteInitializer {

  public static void startUp(ApplicationContext context) {
    WdkInitializer.initializeWdk(context);
    CommentFactoryManager.initializeCommentFactory(context);
    WdkModel wdkModel = WdkInitializer.getWdkModel(context);
    EuPathSiteSetup.initialize(wdkModel);
    ApiSiteEventHandlers.initialize(wdkModel);
    JBrowseQueries.preload();
    DataPlotterQueries.preload();

    // site specific cache clears
    SiteSpecificTmpFileCache.clear(wdkModel, CacheNames.ALL_RECORDS_EXPANDED.SUFFIX);
  }

  public static void shutDown(ApplicationContext context) {
    CommentFactoryManager.terminateCommentFactory(context);
    WdkInitializer.terminateWdk(context);
  }
}
