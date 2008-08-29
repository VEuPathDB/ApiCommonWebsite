/**
 * 
 */
package org.apidb.apicommon.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apidb.apicommon.model.Comment;
import org.gusdb.wdk.controller.CConstants;
import org.gusdb.wdk.model.jspwrap.UserBean;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;

/**
 * @author xingao
 * 
 */
public class ProcessAddCommentAction extends CommentAction {

    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        // get comment factory, and initialize it if necessary

        // get the referer link
        String referer = (String) request.getParameter(CConstants.WDK_REFERER_URL_KEY);
        if (referer == null) referer = request.getHeader("referer");

        int index = referer.lastIndexOf("/");
        referer = referer.substring(index);
        ActionForward forward = new ActionForward(referer, false);
        // forward.setRedirect(true);

        WdkModelBean wdkModel = (WdkModelBean) getServlet().getServletContext().getAttribute(
                CConstants.WDK_MODEL_KEY);
        // get the current user
        UserBean user = (UserBean) request.getSession().getAttribute(
                CConstants.WDK_USER_KEY);
        // if the user is null or is a guest, fail
        if (user == null || user.isGuest()) {
            // This is the case where the session times out while the user is on
            // the
            // comment form page, or someone maliciously trying to post to the
            // comment form
            // action directly. Return to the add comments page, where it is
            // handled correctly.
            return forward;
        }

        // get all the parameters
        // HTML sanitization need to be enabled only for headline and content.
        String headline = request.getParameter("headline");
        if (headline.trim().length() == 0) headline = null;
        else headline = BBCode.getInstance().convertBBCodeToHtml(headline);

        String content = BBCode.getInstance().convertBBCodeToHtml(
                request.getParameter("content"));

        if (headline == null && (content == null || content.length() == 0)) {
            request.setAttribute("submitStatus",
                    "Error: Comment cannot be empty.");
            return forward;
        }

        String commentTarget = request.getParameter("commentTargetId");
        String stableId = request.getParameter("stableId");
        String organism = request.getParameter("organism");

        String extDbName = request.getParameter("externalDbName");
        String extDbVersion = request.getParameter("externalDbVersion");

        String locType = request.getParameter("locType");
        String coordinateType = null;
        boolean reversed = false;
        if (locType.startsWith("genome")) {
            coordinateType = LOCATION_COORDINATETYPE_GENOME;
            if (locType.endsWith("r")) reversed = true;
        } else coordinateType = LOCATION_COORDINATETYPE_PROTEIN;

        String locations = request.getParameter("locations");

        String email = user.getEmail().trim().toLowerCase();
        String projectName = wdkModel.getDisplayName();
        String projectVersion = wdkModel.getVersion();

        // create a comment instance
        Comment comment = new Comment(email);
        comment.setCommentTarget(commentTarget);
        comment.setStableId(stableId);
        comment.setProjectName(projectName);
        comment.setProjectVersion(projectVersion);
        comment.setHeadline(headline);
        comment.setOrganism(organism);
        comment.setContent(content);
        try {
            comment.setLocations(reversed, locations, coordinateType);
        } catch (Exception e) {
            request.setAttribute(
                    "submitStatus",
                    "Error in Location format. "
                            + "Please refer to the format examples on the Add Comment page");
            return forward;
        }
        comment.addExternalDatabase(extDbName, extDbVersion);

        // add the comment
        getCommentFactory().addComment(comment);

        // redirect back to the referer page
        request.setAttribute("submitStatus", "success");
        return forward;
    }
}
