/**
 * 
 */
package org.apidb.apicommon.controller;

import java.net.URL;
import java.io.File;

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
import org.gusdb.wdk.controller.ApplicationInitListener;


/**
 * @author xingao
 *
 */
public class ShowAddCommentAction extends Action {
	public static final String DEFAULT_ADDCOMMENT_PAGE = "addComments.jsp";
	public static final String CUSTOM_ADDCOMMENT_PAGE = "customAddComments.jsp";

    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        // get comment factory, and initialize it if necessary
        CommentFactory factory = null;
        ServletContext application = getServlet().getServletContext();
        try {
            factory = CommentFactory.getInstance();
        } catch (WdkModelException ex) {
            // the comment factory is not initialized yet, do it
            String configXml = application.getInitParameter(ProcessAddCommentAction.COMMENT_CONFIG_XML_PARAM);
            URL configURL = ProcessAddCommentAction.createURL(configXml, ProcessAddCommentAction.DEFAULT_COMMENT_CONFIG_XML,
                    application);
            CommentFactory.initialize(configURL);
            factory = CommentFactory.getInstance();
        }

		String customViewDir = (String)application.getAttribute(CConstants.WDK_CUSTOMVIEWDIR_KEY);
        String addCommentPage = customViewDir + File.separator + CUSTOM_ADDCOMMENT_PAGE;
	
        ActionForward forward = null;

        if (ApplicationInitListener.resourceExists (addCommentPage, application)) {
        	forward = new ActionForward (addCommentPage, false);
        } else {
            forward = new ActionForward (File.separator + DEFAULT_ADDCOMMENT_PAGE, false);
        }

        // get parameters
        String stableId = request.getParameter("stableId");
        String commentTargetId = request.getParameter("commentTargetId");
        
        // get comment target
        CommentTarget commentTarget = factory.getCommentTarget(commentTargetId);
        
		request.setAttribute ("stableId", stableId);
		request.setAttribute ("commentTarget", commentTarget);
		request.setAttribute("externalDbName", request.getParameter("externalDbName"));
		request.setAttribute("externalDbVersion", request.getParameter("externalDbVersion"));
		
        // redirect back to the referer page
        return forward;
    }
}
