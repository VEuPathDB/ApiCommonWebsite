package org.gusdb.wdk.discoverable;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.Cookie;

import org.gusdb.wdk.discoverable.AdditionalLogoutCookies;

public class GbrowseCookies implements AdditionalLogoutCookies {

  private static String[][] COOKIE_INFO = {
      { "gbrowse2_sess", "/cgi-bin/" },
      { "gbrowse2_sess", "/cgi-bin/gbrowse_img" },
      { "authority", "/cgi-bin/" }
  };

  private static List<Cookie> COOKIES = new ArrayList<Cookie>();
  
  static {
    for (String[] cookieData : COOKIE_INFO) {
      Cookie cookie = new Cookie(cookieData[0], "");
      cookie.setPath(cookieData[1]);
      COOKIES.add(cookie);
    }
  }
  
  @Override
  public List<Cookie> getCookies() {
    return COOKIES;
  }
}
