package org.apidb.apicommon.jmx;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.log4j.Logger;

public final class JmxInitListener implements ServletContextListener {

  @SuppressWarnings("unused")
  private static final Logger logger = Logger.getLogger(JmxInitListener.class);
  MBeanRegistration registration;
  
  public void contextInitialized(ServletContextEvent sce) {
    ContextThreadLocal.set(sce.getServletContext());
    registration = new MBeanRegistration();
    registration.init();
    ContextThreadLocal.unset();

  }

  public void contextDestroyed(ServletContextEvent sce) {
    registration.destroy();
  }

}