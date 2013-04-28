package org.apidb.apicommon.controller.action;

import org.gusdb.wdk.controller.action.user.ProcessOpenIdAction;
import org.gusdb.wdk.controller.actionutil.ActionResult;

public class CustomProcessOpenIdAction extends ProcessOpenIdAction {

  @Override
  protected ActionResult getSuccessfulLoginResult(String redirectUrl) {
    return CustomProcessLoginAction.getGbrowseLoginUrl(getWdkModel(), redirectUrl);
  }
}
