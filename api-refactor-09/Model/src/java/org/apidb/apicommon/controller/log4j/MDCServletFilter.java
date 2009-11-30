package org.apidb.apicommon.controller.log4j;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.MDC;

/**
A filter that adds (and removes) MDC variables for log4j.
Use as %X... values in log4j.properties. e.g.
  log4j.appender.R.layout.ConversionPattern=%X{ipAddress}
And in web.xml
  <filter>
    <filter-name>MDCServletFilter</filter-name>
    <filter-class>org.apidb.apicommon.controller.log4j.MDCServletFilter</filter-class>
  </filter>
  
  <filter-mapping>
    <filter-name>MDCServletFilter</filter-name>
    <url-pattern>/*</url-pattern>
  </filter-mapping>
*/
public class MDCServletFilter implements Filter {

  public void init(FilterConfig filterConfig) { }

  public void destroy() { }

  public void doFilter(ServletRequest request,
                       ServletResponse response,
                       FilterChain chain) 
              throws IOException, ServletException {
              
    String ipAddress;
    String sessionId;
    
    try {
      
      ipAddress = request.getRemoteAddr();
      if (ipAddress != null)
          MDC.put("ipAddress", ipAddress);

      HttpSession session = ((HttpServletRequest)request).getSession(false);
      if (session != null) {
        sessionId = session.getId();
        if (sessionId != null)
            MDC.put("sessionId", sessionId);
      }
      
      // Continue processing the rest of the filter chain.
      chain.doFilter(request, response);

    } finally {
    /** should remove data at the end, but if I do that then
        internal classes don't get the info. But, hey, what 
        harm can not cleaning do? **/
      //MDC.remove("ipAddress");
      //MDC.remove("sessionId");
      
    }
  }
}