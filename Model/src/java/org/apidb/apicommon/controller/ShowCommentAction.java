package org.apidb.apicommon.controller;

import java.io.File;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apidb.apicommon.model.Comment;
import org.apidb.apicommon.model.CommentComposer;
import org.apidb.apicommon.model.CommentFactory;
import org.gusdb.wdk.controller.ApplicationInitListener;
import org.gusdb.wdk.controller.CConstants;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;

public class ShowCommentAction extends CommentAction {

    public static final String DEFAULT_COMMENT_PAGE = "showComments.jsp";
    public static final String CUSTOM_COMMENT_PAGE = "customShowComments.jsp";

    public static final String STABLE_ID_KEY = "stable_id";
    public static final String PROJECT_ID_KEY = "project_id";
    public static final String COMMENT_ID_KEY = "comment_id";
    public static final String COMMENT_LIST_KEY = "comments";

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        ServletContext application = getServlet().getServletContext();

        WdkModelBean wdkModel = ( WdkModelBean ) application.getAttribute(
                CConstants.WDK_MODEL_KEY );

        // get the factory
        CommentFactory factory = getCommentFactory();

        // get the comments for the (project_id, stable_id) tuple
        ShowCommentForm commentForm = (ShowCommentForm) form;
        Comment[] comments = factory.queryComments(null, commentForm
                .getProjectId(), commentForm.getStableId(), null, null, null);
        
        // fill in user name and organization
        CommentComposer.fillUserInfo(comments, wdkModel.getModel());
        
        // set for the forwarding page
        request.setAttribute(COMMENT_LIST_KEY, comments);
        request.setAttribute(STABLE_ID_KEY, commentForm.getStableId());
        request.setAttribute(PROJECT_ID_KEY, commentForm.getProjectId());
        
        // construct url
        String customViewDir = (String) application
                .getAttribute(CConstants.WDK_CUSTOMVIEWDIR_KEY);
        String commentPage = customViewDir + File.separator
                + CUSTOM_COMMENT_PAGE;

        // check if custom comment page exist; if not, use the default one
        if (!ApplicationInitListener
                .resourceExists(commentPage, application)) {
            commentPage = File.separator + DEFAULT_COMMENT_PAGE;
        }
        
        // append comment anchor, if comment_id exist
        String commentId = commentForm.getCommentId();
        if (commentId != null && commentId.length() > 0) {
            commentPage += "#" + commentId;
        }
        
        // redirect to the show comments page
        return new ActionForward(commentPage, false);
    }
}
