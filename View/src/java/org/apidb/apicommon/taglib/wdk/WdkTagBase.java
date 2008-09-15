package org.apidb.apicommon.taglib.wdk;


import javax.servlet.ServletRequest;
import javax.servlet.ServletContext;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import javax.servlet.jsp.JspException;


import org.gusdb.wdk.controller.CConstants;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.WdkModelException;


public abstract class WdkTagBase extends SimpleTagSupport {

    protected WdkModelBean wdkModelBean;
    protected WdkModel wdkModel;
    protected int varScope;

    public WdkTagBase() {
        varScope = PageContext.PAGE_SCOPE;
    }

    public void doTag() throws JspException {
        setWdkModelBean();
        setWdkModel();
    }
    
    public void setWdkModel() {
        wdkModel = wdkModelBean.getModel();
    }
    
    public void setWdkModelBean() {
        wdkModelBean = (WdkModelBean) this.getContext().
        getAttribute(CConstants.WDK_MODEL_KEY);
    }

    protected boolean export(String var, Object value) {
        return export(var, value, this.varScope);
    }

    protected boolean export(String var, Object value, int varScope) {
       JspContext jspContext = getJspContext();
       if (value != null)
           jspContext.setAttribute(var, value, varScope);
       else
           jspContext.removeAttribute(var, varScope);
       return true;
    }
    
    protected ServletRequest getRequest() {
        return ((PageContext)getJspContext()).getRequest();
    }

    protected ServletContext getContext() {
        return ((PageContext)getJspContext()).
                  getServletConfig().getServletContext();
    }

    protected ServletContext getApplication() {
        return getContext();
    }
}