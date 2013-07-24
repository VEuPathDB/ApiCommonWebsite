package org.apidb.apicommon.controller.action;

import org.gusdb.wdk.controller.action.user.ProcessLoginAction;
import org.gusdb.wdk.controller.actionutil.ActionResult;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;

public class CustomProcessLoginAction extends ProcessLoginAction {

  @Override
  protected ActionResult getSuccessfulLoginResult(String redirectUrl, int wdkCookieMaxAge) {
    return CustomProcessLoginAction.getGbrowseLoginUrl(getWdkModel(), redirectUrl, wdkCookieMaxAge);
  }

  static ActionResult getGbrowseLoginUrl(WdkModelBean model, String redirectUrl, int wdkCookieMaxAge) {
    String loginPageUrl = new StringBuffer("/gbrowse/gbrowseSetup.html?redirectUrl=")
    	.append(redirectUrl).append("&project=").append(model.getName().toLowerCase())
    	.append("&cookieMaxAge=").append(wdkCookieMaxAge).toString();
    return new ActionResult().setExternalPath(loginPageUrl);
  }
}
