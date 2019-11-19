package org.apidb.apicommon.server;

import java.net.URI;

import org.apidb.apicommon.service.ApiWebServiceApplication;
import org.glassfish.jersey.server.ResourceConfig;
import org.gusdb.fgputil.server.RESTServer;
import org.gusdb.fgputil.web.ApplicationContext;
import org.json.JSONObject;

public class ApiWebsiteServer extends RESTServer {

  public static void main(String[] args) {
    new ApiWebsiteServer(args).start();
  }

  public ApiWebsiteServer(String[] commandLineArgs) {
    super(commandLineArgs);
  }

  @Override
  protected ResourceConfig getResourceConfig() {
    return new ResourceConfig().registerClasses(
        new ApiWebServiceApplication().getClasses());
  }

  @Override
  protected ApplicationContext createApplicationContext(URI serviceUri, JSONObject config) {
    return new ApiApplicationContext(serviceUri, config);
  }

}
