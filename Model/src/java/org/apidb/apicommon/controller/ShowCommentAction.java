package org.apidb.apicommon.controller;

import java.io.File;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apidb.apicommon.model.Comment;
import org.apidb.apicommon.model.CommentFactory;
import org.gusdb.wdk.controller.ApplicationInitListener;
import org.gusdb.wdk.controller.CConstants;

public class ShowCommentAction extends CommentAction {

    public static final String DEFAULT_COMMENT_PAGE = "addComments.jsp";
    public static final String CUSTOM_COMMENT_PAGE = "customAddComments.jsp";

    public static final String COMMENT_KEY = "comment";
    
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        ServletContext application = getServlet().getServletContext();
        
        // get the factory
        CommentFactory factory = getCommentFactory();

        // get the comment
        ShowCommentForm commentForm = (ShowCommentForm) form;
        String projectId = commentForm.getProjectId();
        int commentId = commentForm.getCommentId();
        Comment comment = factory.getComment(commentId);
        request.setAttribute(COMMENT_KEY, comment);
        
        String customViewDir = (String) application
                .getAttribute(CConstants.WDK_CUSTOMVIEWDIR_KEY);
        String addCommentPage = customViewDir + File.separator
                + CUSTOM_COMMENT_PAGE;

        ActionForward forward = null;

        if (ApplicationInitListener.resourceExists(addCommentPage, application)) {
            forward = new ActionForward(addCommentPage, false);
        } else {
            forward = new ActionForward(File.separator
                    + DEFAULT_COMMENT_PAGE, false);
        }

        // redirect back to the referer page
        return forward;
    }

}
