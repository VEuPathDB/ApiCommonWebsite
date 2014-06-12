package org.apidb.apicommon.controller;

import org.eupathdb.common.model.InstanceManager;
import org.gusdb.wdk.controller.ApplicationInitListener;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;

public class EuPathContextListener extends ApplicationInitListener {

  /**
   * get wdk model from singleton manager instead.
   * 
   * @see org.gusdb.wdk.controller.ApplicationInitListener#createWdkModel(java.lang.String, java.lang.String)
   */
  @Override
  protected WdkModel createWdkModel(String project, String gusHome) throws WdkModelException {
    return InstanceManager.getInstance(WdkModel.class, project);
  }

}
