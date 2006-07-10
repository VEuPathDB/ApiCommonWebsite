/**
 * 
 */
package org.apidb.apicommon.controller;

import java.net.URL;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apidb.apicommon.model.CommentFactory;
import org.apidb.apicommon.model.CommentTarget;
import org.gusdb.wdk.controller.CConstants;
import org.gusdb.wdk.model.WdkModelException;


/**
 * @author xingao
 *
 */
public class ShowAddCommentAction extends Action {
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        // get comment factory, and initialize it if necessary
        CommentFactory factory = null;
        try {
            factory = CommentFactory.getInstance();
        } catch (WdkModelException ex) {
            // the comment factory is not initialized yet, do it
            ServletContext application = getServlet().getServletContext();
            String configXml = application.getInitParameter(ProcessAddCommentAction.COMMENT_CONFIG_XML_PARAM);
            URL configURL = ProcessAddCommentAction.createURL(configXml, ProcessAddCommentAction.DEFAULT_COMMENT_CONFIG_XML,
                    application);
            CommentFactory.initialize(configURL);
            factory = CommentFactory.getInstance();
        }

        // get the referer link
        String referer = (String) request.getParameter(CConstants.WDK_REFERER_URL_KEY);
        if (referer == null) referer = request.getHeader("referer");

        int index = referer.lastIndexOf("/");
        referer = referer.substring(index);
        ActionForward forward = new ActionForward(referer);
        forward.setRedirect(true);
        
        // get parameters
        String stableId = request.getParameter("stableId");
        String commentTargetId = request.getParameter("commentTarget");
        
        // get comment target
        CommentTarget commentTarget = factory.getCommentTarget(commentTargetId);
        
        // set the values to the action form
        ShowAddCommentForm commentForm = (ShowAddCommentForm) form;
        commentForm.setCommentTarget(commentTarget);
        commentForm.setStableId(stableId);

        // redirect back to the referer page
        return forward;
    }
}
