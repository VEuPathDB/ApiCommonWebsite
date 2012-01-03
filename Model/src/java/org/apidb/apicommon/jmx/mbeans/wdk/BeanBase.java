package org.apidb.apicommon.jmx.mbeans.wdk;

import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;
import org.gusdb.wdk.controller.CConstants;
import org.apidb.apicommon.jmx.ContextThreadLocal;
import javax.servlet.ServletContext;
import org.apache.log4j.Logger;

public abstract class BeanBase {

  WdkModelBean wdkModelBean;
  WdkModel wdkModel;
  ServletContext context;
  protected static final Logger logger = Logger.getLogger(BeanBase.class);
  
  public BeanBase() {
    context = ContextThreadLocal.get();
    wdkModelBean = (WdkModelBean)context.getAttribute(CConstants.WDK_MODEL_KEY);
    wdkModel = wdkModelBean.getModel();
  }

}