package org.apidb.apicommon.jmx.util;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.gusdb.wdk.controller.CConstants;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;

public class Util {

  @SuppressWarnings("unused")
  private static final Logger logger = Logger.getLogger(Util.class.getName());
  
  protected ServletContext context;
  protected WdkModelBean wdkModelBean;
  protected WdkModel wdkModel;

  public Util(ServletContext context) {
    this.context = context;
    wdkModelBean = (WdkModelBean) context.getAttribute(CConstants.WDK_MODEL_KEY);
    wdkModel = wdkModelBean.getModel();
  }

  public WdkModelBean getWdkModelBean() {
    return wdkModelBean;
  }
  
  public WdkModel getWdkModel() {
    return wdkModel;
  }

  public ServletContext getApplication() {
    return context;
  }
}
