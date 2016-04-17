package org.apidb.apicommon.errors;

import java.util.Map;
import java.util.regex.Pattern;

import org.gusdb.wdk.controller.actionutil.RequestData;
import org.gusdb.wdk.model.WdkModel;

public class ErrorContext {
  
    private final WdkModel _wdkModel;
    private final String _projectName;
    private final RequestData _requestData;
    private final Map<String, Object> _servletContextAttributes;
    private final Map<String, Object> _requestAttributeMap;
    private final Map<String, Object> _sessionAttributeMap;
    
    public ErrorContext(WdkModel wdkModel,
            String projectName, RequestData requestData,
            Map<String, Object> servletContextAttributes,
            Map<String, Object> requestAttributeMap,
            Map<String, Object> sessionAttributeMap) {
        _wdkModel = wdkModel;
        _projectName = projectName;
        _requestData = requestData;
        _servletContextAttributes = servletContextAttributes;
        _requestAttributeMap = requestAttributeMap;
        _sessionAttributeMap = sessionAttributeMap;
    }

    public String getProjectName() { return _projectName; }
    public RequestData getRequestData() { return _requestData; }
    public Map<String, Object> getServletContextAttributes() { return _servletContextAttributes; }
    public Map<String, Object> getRequestAttributeMap() { return _requestAttributeMap; }
    public Map<String, Object> getSessionAttributeMap() { return _sessionAttributeMap; }

    /**
     * Whether or not the site is monitored now depends on whether administrator email(s) exist - CWL 13APR16
     * @return - true if the site is monitored and false otherwise
     */
    public boolean siteIsMonitored() {
      String emailProp = _wdkModel.getModelConfig().getAdminEmail();
      return emailProp != null && !emailProp.isEmpty();
    }
    
    public String[] getAdminEmails() {
        // Replacing SITE_ADMIN_EMAIL from model.prop with ADMIN_EMAIL from model-config.xml - CWL 13APR16 
        //String emailProp = _wdkModel.getProperties().get("SITE_ADMIN_EMAIL");
        String emailProp = _wdkModel.getModelConfig().getAdminEmail();
        return (emailProp == null || emailProp.isEmpty() ? new String[]{} :
            Pattern.compile("[,\\s]+").split(emailProp));
    }
}
