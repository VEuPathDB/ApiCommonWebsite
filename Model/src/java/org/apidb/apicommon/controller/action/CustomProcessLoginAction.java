package org.apidb.apicommon.controller.action;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.gusdb.wdk.controller.CConstants;
import org.gusdb.wdk.controller.action.user.ProcessLoginAction;
import org.gusdb.wdk.controller.actionutil.ActionResult;
import org.gusdb.wdk.controller.actionutil.RequestData;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;

public class CustomProcessLoginAction extends ProcessLoginAction {

  private static final String GBROWSE_SETUP_PAGE = "/gbrowse/gbrowseSetup.html";
  
  @Override
  protected ActionResult getSuccessfulLoginResult(String redirectUrl, int wdkCookieMaxAge) {
    return getGbrowseLoginUrl(getWdkModel(), redirectUrl, wdkCookieMaxAge, getCurrentUserOrNull().getFirstName());
  }
  
  @Override
  protected ActionResult getFailedLoginResult(Exception e) {
    return getFailedLoginUrl(getRequestData(), getOriginalReferrer(getParams(), getRequestData()), e);
  }

  static ActionResult getGbrowseLoginUrl(WdkModelBean model, String redirectUrl, int wdkCookieMaxAge, String displayName) {
    return new ActionResult().setExternalPath(
        getGbrowsePathWithParams(redirectUrl, model.getName().toLowerCase(),
            wdkCookieMaxAge, displayName));
  }

  static ActionResult getFailedLoginUrl(RequestData reqData, String originalReferrer, Exception e) {
    StringBuffer loginPageUrl = new StringBuffer(reqData.getWebAppBaseUrl());
    if (originalReferrer.contains("showLogin.do")) {
      // avoid continual appending of showLogin.co?redirect=/showLogin.do... etc.
      loginPageUrl = new StringBuffer(originalReferrer);
    }
    else {
      String encodedReferrer = urlEncodeUtf8(originalReferrer);
      String errorText = urlEncodeUtf8(e.getMessage());
      if (e instanceof WdkUserException) {
        loginPageUrl.append("/showLogin.do?")
            .append(CConstants.WDK_REDIRECT_URL_KEY).append("=").append(encodedReferrer).append("&")
            .append(CConstants.WDK_LOGIN_ERROR_KEY).append("=").append(errorText);
      }
      else {
        loginPageUrl.append("/showErrorPage.do?")
            .append(CConstants.WDK_ERROR_TEXT_KEY).append("=").append(errorText);
      }
    }
    return new ActionResult().setExternalPath(getGbrowsePathWithParams(loginPageUrl.toString(), "", -1, ""));
  }

  private static String getGbrowsePathWithParams(String redirectUrl, String project, int wdkCookieMaxAge, String displayName) {
    return new StringBuffer(GBROWSE_SETUP_PAGE)
        .append("?redirectUrl=").append(urlEncodeUtf8(redirectUrl))
        .append("&project=").append(project)
        .append("&cookieMaxAge=").append(wdkCookieMaxAge)
        .append("&userDisplayName=").append(urlEncodeUtf8(displayName))
        .toString();
  }
  
  public static String urlEncodeUtf8(String str) {
    try {
      return URLEncoder.encode(str, "UTF-8");
    }
    catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}
