package org.apidb.apicommon.errors;

import java.util.Map;
import java.util.regex.Pattern;

import org.gusdb.wdk.controller.actionutil.RequestData;
import org.gusdb.wdk.model.WdkModel;

public class ErrorContext {

    private final String[] _publicSitePrefixes;
    private final WdkModel _wdkModel;
    private final String _projectName;
    private final RequestData _requestData;
    private final Map<String, Object> _servletContextAttributes;
    private final Map<String, Object> _requestAttributeMap;
    private final Map<String, Object> _sessionAttributeMap;
    
    public ErrorContext(String[] publicSitePrefixes, WdkModel wdkModel,
            String projectName, RequestData requestData,
            Map<String, Object> servletContextAttributes,
            Map<String, Object> requestAttributeMap,
            Map<String, Object> sessionAttributeMap) {
        _publicSitePrefixes = publicSitePrefixes;
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

    public boolean isPublicSite() {
        String serverName = _requestData.getServerName();
        for (String prefix : _publicSitePrefixes) {
            if ((prefix + _projectName + ".org").equalsIgnoreCase(serverName)) {
                return true;
            }
        }
        return false;
    }

    public boolean siteIsMonitored() {
        return isPublicSite();
    }
    
    public String[] getAdminEmails() {
        String emailProp = _wdkModel.getProperties().get("SITE_ADMIN_EMAIL");
        return (emailProp == null || emailProp.isEmpty() ? new String[]{} :
            Pattern.compile("[,\\s]+").split(emailProp));
    }
}
