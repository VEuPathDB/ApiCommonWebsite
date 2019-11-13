package org.apidb.apicommon.server;

import java.io.IOException;

import org.apidb.apicommon.controller.ApiSiteInitializer;
import org.gusdb.wdk.controller.WdkApplicationContext;
import org.json.JSONObject;

public class ApiApplicationContext extends WdkApplicationContext {

  private static final String GUS_HOME_KEY = "gusHome";
  private static final String PROJECT_ID_KEY = "projectId";

  public ApiApplicationContext(JSONObject config) {
    super(
        // basically the replacement for config contained in web.xml; set init parameters
        config.getString(GUS_HOME_KEY),
        config.getString(PROJECT_ID_KEY),
        "/service"
    );
  }

  @Override
  protected void initialize() {
    ApiSiteInitializer.startUp(this);
  }

  @Override
  public void close() throws IOException {
    ApiSiteInitializer.shutDown(this);
  }

}
