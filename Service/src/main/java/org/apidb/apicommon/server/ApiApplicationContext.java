package org.apidb.apicommon.server;

import java.io.IOException;

import org.apidb.apicommon.controller.ApiSiteInitializer;
import org.gusdb.fgputil.server.BasicApplicationContext;
import org.gusdb.wdk.controller.WdkInitializer;
import org.gusdb.wdk.model.Utilities;
import org.json.JSONObject;

public class ApiApplicationContext extends BasicApplicationContext {

  private static final String GUS_HOME_KEY = "gusHome";
  private static final String PROJECT_ID_KEY = "projectId";
  private static final String SERVICE_ENDPOINT_KEY = "wdkServiceEndpoint";

  public ApiApplicationContext(JSONObject config) {
    // basically the replacement for config contained in web.xml; set init parameters
    setInitParameter(WdkInitializer.GUS_HOME_KEY, config.getString(GUS_HOME_KEY));
    setInitParameter(SERVICE_ENDPOINT_KEY, config.getString(SERVICE_ENDPOINT_KEY));
    setInitParameter(Utilities.ARGUMENT_PROJECT_ID, config.getString(PROJECT_ID_KEY));

    // initialize the application
    ApiSiteInitializer.startUp(this);
  }

  @Override
  public void close() throws IOException {
    ApiSiteInitializer.shutDown(this);
  }

}
