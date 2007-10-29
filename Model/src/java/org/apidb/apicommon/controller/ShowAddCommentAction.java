/**
 * 
 */
package org.apidb.apicommon.controller;

import java.io.File;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apidb.apicommon.model.CommentFactory;
import org.apidb.apicommon.model.CommentTarget;
import org.gusdb.wdk.controller.ApplicationInitListener;
import org.gusdb.wdk.controller.CConstants;

/**
 * @author xingao
 * 
 */
public class ShowAddCommentAction extends CommentAction {

    public static final String DEFAULT_ADD_COMMENT_PAGE = "addComments.jsp";
    public static final String CUSTOM_ADD_COMMENT_PAGE = "customAddComments.jsp";

    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        ServletContext application = getServlet().getServletContext();
        // get comment factory, and initialize it if necessary
        CommentFactory factory = getCommentFactory();

        String customViewDir = (String) application
                .getAttribute(CConstants.WDK_CUSTOMVIEWDIR_KEY);
        String addCommentPage = customViewDir + File.separator
                + CUSTOM_ADD_COMMENT_PAGE;

        ActionForward forward = null;

        if (ApplicationInitListener.resourceExists(addCommentPage, application)) {
            forward = new ActionForward(addCommentPage, false);
        } else {
            forward = new ActionForward(File.separator
                    + DEFAULT_ADD_COMMENT_PAGE, false);
        }

        // get parameters
        String stableId = request.getParameter("stableId");
        String commentTargetId = request.getParameter("commentTargetId");
        String organism = request.getParameter("organism");

        // get comment target
        CommentTarget commentTarget = factory.getCommentTarget(commentTargetId);
    
        request.setAttribute("organism", organism);
        request.setAttribute("stableId", stableId);
        request.setAttribute("commentTarget", commentTarget);
        request.setAttribute("externalDbName", request
                .getParameter("externalDbName"));
        request.setAttribute("externalDbVersion", request
                .getParameter("externalDbVersion"));

        // redirect back to the referer page
        return forward;
    }
}
