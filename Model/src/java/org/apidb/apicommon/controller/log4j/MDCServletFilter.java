package org.apidb.apicommon.controller.log4j;

import java.io.*;
import javax.servlet.*;
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
    try {
    
      MDC.put("ipAddress", request.getRemoteAddr() );

      // Continue processing the rest of the filter chain.
      chain.doFilter(request, response);

    } finally {
      // always clean up the variables associated with this request
      MDC.remove("ipAddress");
    }
  }
}