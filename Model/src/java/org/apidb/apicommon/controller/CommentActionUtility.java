/**
 * 
 */
package org.apidb.apicommon.controller;

import javax.servlet.ServletContext;

import org.apidb.apicommon.model.comment.CommentFactory;
import org.apidb.apicommon.model.comment.CommentModelException;
import org.apidb.apicommon.model.userfile.UserFileFactory;
import org.eupathdb.common.model.InstanceManager;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModelException;

/**
 * @author xingao
 * 
 */
public class CommentActionUtility {

  public static final String COMMENT_FACTORY_KEY = "comment-factory";

  public static CommentFactory getCommentFactory(ServletContext context) throws WdkModelException,
      CommentModelException {
    CommentFactory factory = (CommentFactory) context.getAttribute(COMMENT_FACTORY_KEY);
    if (factory == null) {
      String projectId = context.getInitParameter(Utilities.ARGUMENT_PROJECT_ID);
      factory = InstanceManager.getInstance(CommentFactory.class, projectId);
      context.setAttribute(COMMENT_FACTORY_KEY, factory);
    }
    return factory;
  }

  public static UserFileFactory getUserFileFactory(ServletContext context) throws WdkModelException,
      CommentModelException {
    String projectId = context.getInitParameter(Utilities.ARGUMENT_PROJECT_ID);
    return InstanceManager.getInstance(UserFileFactory.class, projectId);
  }
}
