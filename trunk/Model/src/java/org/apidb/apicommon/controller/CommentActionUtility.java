/**
 * 
 */
package org.apidb.apicommon.controller;

import javax.servlet.ServletContext;

import org.apidb.apicommon.model.CommentFactory;
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
            String gusHome = context.getRealPath(context.getInitParameter(Utilities.SYSTEM_PROPERTY_GUS_HOME));
            String projectId = context.getInitParameter(Utilities.ARGUMENT_PROJECT_ID);
            factory = CommentFactory.getInstance(gusHome, projectId);
            context.setAttribute(COMMENT_FACTORY_KEY, factory);
        }
        return factory;
    }

}
