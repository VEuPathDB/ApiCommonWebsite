package org.apidb.apicommon.controller;

import javax.servlet.ServletContext;

import org.apidb.apicommon.model.comment.CommentFactory;
import org.apidb.apicommon.model.userfile.UserFileFactory;
import org.gusdb.fgputil.runtime.InstanceManager;
import org.gusdb.wdk.model.Utilities;

/**
 * @author xingao
 * 
 */
public class CommentActionUtility {

  public static final String COMMENT_FACTORY_KEY = "comment-factory";

  public static CommentFactory getCommentFactory(ServletContext context) {
    CommentFactory factory = (CommentFactory) context.getAttribute(COMMENT_FACTORY_KEY);
    if (factory == null) {
      String projectId = context.getInitParameter(Utilities.ARGUMENT_PROJECT_ID);
      factory = InstanceManager.getInstance(CommentFactory.class, projectId);
      context.setAttribute(COMMENT_FACTORY_KEY, factory);
    }
    return factory;
  }

  public static UserFileFactory getUserFileFactory(ServletContext context) {
    String projectId = context.getInitParameter(Utilities.ARGUMENT_PROJECT_ID);
    return InstanceManager.getInstance(UserFileFactory.class, projectId);
  }
}
