package org.apidb.apicommon.service.services;

import java.io.OutputStream;

import org.apidb.apicommon.controller.SiteSpecificTmpFileCache;
import org.apidb.apicommon.controller.SiteSpecificTmpFileCache.CacheName;
import org.gusdb.fgputil.functional.FunctionalInterfaces.ConsumerWithException;
import org.gusdb.fgputil.runtime.GusHome;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.service.service.OntologyService;

public class ApiOntologyService extends OntologyService {

  @Override
  protected ConsumerWithException<OutputStream> getCategoriesOntologyJsonStreamer(WdkModel wdkModel) throws WdkModelException {
    // use cache mechanism for efficient delivery of categories ontology
    return SiteSpecificTmpFileCache.get(wdkModel, CacheName.CATEGORIES_ONTOLOGY, () -> getCategoriesOntologyJson(wdkModel));
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
