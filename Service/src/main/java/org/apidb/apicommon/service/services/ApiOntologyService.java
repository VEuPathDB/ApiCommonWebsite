package org.apidb.apicommon.service.services;

import java.io.OutputStream;
import java.util.Collections;

import org.apache.log4j.Logger;
import org.apidb.apicommon.controller.SiteSpecificTmpFileCache;
import org.apidb.apicommon.controller.SiteSpecificTmpFileCache.CacheName;
import org.gusdb.fgputil.functional.FunctionalInterfaces.ConsumerWithException;
import org.gusdb.fgputil.runtime.GusHome;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.service.service.OntologyService;

public class ApiOntologyService extends OntologyService {

  private static final Logger LOG = Logger.getLogger(ApiRecordService.class);

  @Override
  protected ConsumerWithException<OutputStream> getCategoriesOntologyJsonStreamer(WdkModel wdkModel) {
    try {
      // try to use cache mechanism for efficient delivery of categories ontology
      return SiteSpecificTmpFileCache.get(wdkModel, CacheName.CATEGORIES_ONTOLOGY, () -> getCategoriesOntologyJson(wdkModel));
    }
    catch (Exception e) {
      // don't let an exception prevent delivery of data to the client; log and trigger email
      LOG.error("Unable to read cache for categories ontology JSON data", e);
      triggerErrorEvents(Collections.singletonList(e));
      return super.getCategoriesOntologyJsonStreamer(wdkModel);
    }
  }

  public static void cacheCategoriesOntologyJson(WdkModel wdkModel, boolean useSubprocess) {
    SiteSpecificTmpFileCache.put(wdkModel, CacheName.CATEGORIES_ONTOLOGY,
        () -> getCategoriesOntologyJson(wdkModel), useSubprocess, ApiOntologyService.class);
  }

  public static void main(String[] args) throws WdkModelException {
    if (args.length != 1)
      throw new IllegalArgumentException("This tool requires a single argument, project_id");
    try (WdkModel wdkModel = WdkModel.construct(args[0], GusHome.getGusHome())) {
      cacheCategoriesOntologyJson(wdkModel, false);
    }
  }

}
