/**
 * 
 */
package org.apidb.apicommon.controller;

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apidb.apicommon.model.Comment;
import org.apidb.apicommon.model.CommentFactory;
import org.gusdb.wdk.controller.CConstants;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.jspwrap.UserBean;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;

/**
 * @author xingao
 * 
 */
public class ProcessAddCommentAction extends Action {

    public static final String DEFAULT_COMMENT_CONFIG_XML = "/WEB-INF/wdk-model/config/comment-config.xml";
    public static final String COMMENT_CONFIG_XML_PARAM = "commentConfigXml_param";

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
            String configXml = application.getInitParameter(COMMENT_CONFIG_XML_PARAM);
            URL configURL = createURL(configXml, DEFAULT_COMMENT_CONFIG_XML,
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

        WdkModelBean wdkModel = (WdkModelBean) getServlet().getServletContext().getAttribute(
                CConstants.WDK_MODEL_KEY);
        // get the current user
        UserBean user = (UserBean) request.getSession().getAttribute(
                CConstants.WDK_USER_KEY);
        // if the user is null or is a guest, fail
        if (user == null || user.getGuest())
            throw new WdkUserException("Please login before posting a comment.");

        // get the information
        String headline = request.getParameter("headline");
        String content = request.getParameter("content");
        String commentTarget = request.getParameter("commentTarget");
        String stableId = request.getParameter("stableId");
        String reversedStr = request.getParameter("reversed");
        boolean reversed = (reversedStr != null && reversedStr.equalsIgnoreCase("true"))
                ? true
                : false;
        String locations = request.getParameter("locations");

        String email = user.getEmail().trim().toLowerCase();
        String projectName = wdkModel.getName();
        String projectVersion = wdkModel.getVersion();

        // create a comment instance
        Comment comment = new Comment(email);
        comment.setCommentTarget(commentTarget);
        comment.setStableId(stableId);
        comment.setProjectName(projectName);
        comment.setProjectVersion(projectVersion);
        comment.setHeadline(headline);
        comment.setContent(content);
        comment.setLocations(reversed, locations);

        // add the comment
        factory.addComment(comment);

        // redirect back to the referer page
        return forward;
    }

    public static URL createURL(String param, String defaultLoc,
            ServletContext application) {

        if (param == null) {
            param = defaultLoc;
        }

        URL ret = null;
        try {
            ret = application.getResource(param);
            if (ret == null) {
                RuntimeException e = new RuntimeException(
                        "Missing resource. Unable to create URL from " + param);
                throw e;
            }
        } catch (MalformedURLException exp) {
            RuntimeException e = new RuntimeException(exp);
            throw e;
        }
        return ret;
    }

}
