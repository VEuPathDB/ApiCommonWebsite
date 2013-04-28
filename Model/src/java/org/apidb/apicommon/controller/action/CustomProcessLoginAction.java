package org.apidb.apicommon.controller.action;

import org.gusdb.wdk.controller.action.user.ProcessLoginAction;
import org.gusdb.wdk.controller.actionutil.ActionResult;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;

public class CustomProcessLoginAction extends ProcessLoginAction {

  @Override
  protected ActionResult getSuccessfulLoginResult(String redirectUrl) {
    return CustomProcessLoginAction.getGbrowseLoginUrl(getWdkModel(), redirectUrl);
  }

  static ActionResult getGbrowseLoginUrl(WdkModelBean model, String redirectUrl) {
    String loginPageUrl = new StringBuffer("/gbrowse/gbrowseSetup.html?project=")
        .append(model.getName().toLowerCase()).append("&redirectUrl=").append(redirectUrl).toString();
    return new ActionResult().setExternalPath(loginPageUrl);
  }
}
