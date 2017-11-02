package org.apidb.apicommon.service.services;

import static org.gusdb.fgputil.FormatUtil.urlEncodeUtf8;

import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.jspwrap.UserBean;
import org.gusdb.wdk.service.service.SessionService;

public class ApiSessionService extends SessionService {
	
  private static final String GBROWSE_SETUP_PAGE = "/gbrowse/gbrowseSetup.html";

  protected Response createSuccessResponse(String redirectUrl, NewCookie cookie) throws WdkModelException {
	UserBean userBean = this.getSessionUserBean();  
	String gbrowseLoginUrl = getGbrowsePathWithParams(redirectUrl, getWdkModel().getProjectId(), cookie.getMaxAge(), userBean.getFirstName());
	return super.createSuccessResponse(gbrowseLoginUrl, cookie);
  }
  
  private static String getGbrowsePathWithParams(String redirectUrl, String project, int wdkCookieMaxAge, String displayName) {
    return new StringBuffer(GBROWSE_SETUP_PAGE)
            .append("?redirectUrl=").append(urlEncodeUtf8(redirectUrl))
            .append("&project=").append(project)
            .append("&cookieMaxAge=").append(wdkCookieMaxAge)
            .append("&userDisplayName=").append(urlEncodeUtf8(displayName))
            .toString();
  }

}
