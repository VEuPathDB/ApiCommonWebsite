package org.apidb.apicommon.jmx.mbeans.wdk;

public class ServletVersions extends BeanBase implements ServletVersionsMBean   {

  /**
   * Version of the Tomcat server.
   * e.g. Apache Tomcat/5.5.30
   */
  public String getServerInfo() {
    return context.getServerInfo();
  }

  /**
   *  version number of the JSP specification that is supported by the JSP engine
   */
  public String getJspSpecVersion()  {
    javax.servlet.jsp.JspFactory f = javax.servlet.jsp.JspFactory.getDefaultFactory();
    if ( f != null) {
      return f.getEngineInfo().getSpecificationVersion();
    }
    return null;
  }
  
  /**
   * Servlet API version that the servlet container supports.
   */
  public String getServletApiVersion() {
    return context.getMajorVersion() + "." + context.getMinorVersion();
  }

}

/**
app.serverInfo
    Servlet container:	Apache Tomcat/5.5.30
        application.getServerInfo();

app.servletInfo
    Servlet info:	Jasper JSP 2.0 Engine
        ((Servlet)pageContext.getPage()).getServletInfo();
        
app.servletApiVersion
    Servlet API version:	2.4
        application.getMajorVersion() + "." + application.getMinorVersion();

**/
