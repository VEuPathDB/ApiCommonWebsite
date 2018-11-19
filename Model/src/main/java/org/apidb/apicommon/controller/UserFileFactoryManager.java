package org.apidb.apicommon.controller;

import javax.servlet.ServletContext;

import org.apidb.apicommon.model.userfile.UserFileFactory;
import org.gusdb.fgputil.runtime.InstanceManager;
import org.gusdb.wdk.model.Utilities;

public class UserFileFactoryManager {

  public static UserFileFactory getUserFileFactory(ServletContext context) {
    String projectId = context.getInitParameter(Utilities.ARGUMENT_PROJECT_ID);
    return InstanceManager.getInstance(UserFileFactory.class, projectId);
  }
}
