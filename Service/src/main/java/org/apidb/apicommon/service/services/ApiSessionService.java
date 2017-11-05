package org.apidb.apicommon.service.services;

import static org.gusdb.fgputil.FormatUtil.urlEncodeUtf8;

import javax.servlet.http.Cookie;

import org.gusdb.wdk.model.jspwrap.UserBean;
import org.gusdb.wdk.model.user.User;
import org.gusdb.wdk.service.service.SessionService;

public class ApiSessionService extends SessionService {

  private static final String GBROWSE_SETUP_PAGE = "/gbrowse/gbrowseSetup.html";

  @Override
  protected String getSuccessRedirectUrl(String redirectUrl, User user, Cookie cookie) {
    return new StringBuilder(GBROWSE_SETUP_PAGE)
      .append("?redirectUrl=").append(urlEncodeUtf8(redirectUrl))
      .append("&project=").append(getWdkModel().getProjectId())
      .append("&cookieMaxAge=").append(cookie.getMaxAge())
      .append("&userDisplayName=").append(urlEncodeUtf8(new UserBean(user).getFirstName()))
      .toString();
  }
}
