package org.apidb.apicommon.controller;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.gusdb.wdk.controller.WdkInitializer;

/**
 * A class that is initialized at the start of the web application. This makes
 * sure global resources are available to all the contexts that need them
 * 
 */
public class ApplicationInitListener implements ServletContextListener {

  private static final Logger LOG = Logger.getLogger(ApplicationInitListener.class);

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    WdkInitializer.initializeWdk(sce.getServletContext());
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    ServletContext context = sce.getServletContext();
    try {
      CommentActionUtility.getCommentFactory(context).close();
    }
    catch (Exception e) {
      LOG.error("Error while closing CommentFactory (comments db)", e);
    }
    WdkInitializer.terminateWdk(context);
  }
}

