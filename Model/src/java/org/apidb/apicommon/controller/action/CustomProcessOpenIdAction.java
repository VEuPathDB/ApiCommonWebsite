package org.apidb.apicommon.controller.action;

import org.gusdb.wdk.controller.action.user.ProcessLoginAction;
import org.gusdb.wdk.controller.action.user.ProcessOpenIdAction;
import org.gusdb.wdk.controller.actionutil.ActionResult;

public class CustomProcessOpenIdAction extends ProcessOpenIdAction {

  @Override
  protected ActionResult getSuccessfulLoginResult(String redirectUrl, int wdkCookieMaxAge) {
    return CustomProcessLoginAction.getGbrowseLoginUrl(getWdkModel(), redirectUrl,
        wdkCookieMaxAge, getCurrentUserOrNull().getFirstName());
  }
  
  @Override
  protected ActionResult getFailedLoginResult(Exception e) {
    return CustomProcessLoginAction.getFailedLoginUrl(getRequestData(),
        ProcessLoginAction.getOriginalReferrer(getParams(), getRequestData()), e);
  }
}
