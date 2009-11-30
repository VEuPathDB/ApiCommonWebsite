package org.apidb.apicommon.controller;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apidb.apicommon.model.CommentFactory;
import org.apidb.apicommon.model.Comment;
import org.apidb.apicommon.model.Location;
import org.apidb.apicommon.model.ExternalDatabase;

public class EditCommentAction extends CommentAction {

    @Override
    public ActionForward execute(ActionMapping mapping, 
                                 ActionForm form,
                                 HttpServletRequest request, 
                                 HttpServletResponse response)
            throws Exception {
        ServletContext application = getServlet().getServletContext();

        // get the factory
        ServletContext context = servlet.getServletContext();
        CommentFactory factory = CommentActionUtility.getCommentFactory(context);

        // get the comments for the (project_id, stable_id) tuple
        EditCommentForm editForm = (EditCommentForm) form;
        Comment comment = factory.getComment(Integer.valueOf(editForm.getCommentId()).intValue());

        // set for the forwarding page
        String stableId  = editForm.getStableId();
        String projectId = editForm.getProjectId();
        String email = editForm.getEmail();
        String commentId = editForm.getCommentId();
        String commentTargetId = editForm.getCommentTargetId();

        Location[] locs = comment.getLocations();
        String locString = "";
        for(int i = 0; i < locs.length; i++) {
            locString += locs[i].toString() + ",";
        }

        if(locString != "") {

          locString = locString.substring(0, locString.lastIndexOf(","));
        }

        ExternalDatabase[] edbs =  comment.getExternalDbs();
        ExternalDatabase edb = (ExternalDatabase)edbs[0];

        int[] categoryIds = comment.getTargetCategoryIds();
        String categoryString = "";
        if(categoryIds != null && categoryIds.length > 0) {
          for(int i = 0; i < categoryIds.length; i++) {
           categoryString += "&targetCategory=" + categoryIds[i]; 
          }
        }

        String[] pmIds = comment.getPmIds();
        String pmIdString = "";
        if(pmIds != null && pmIds.length > 0) {
          pmIdString += "&pmIds=";
          for(int i = 0; i < pmIds.length; i++) {
            pmIdString += pmIds[i] + " "; 
          }
        }

        String[] accessions = comment.getAccessions();
        String accessionsString = "";
        if(accessions != null && accessions.length > 0) {
          accessionsString += "&accessions=";
          for(int i = 0; i < accessions.length; i++) {
            accessionsString += accessions[i] + " "; 
          }
        }

        String[] associatedIds = comment.getAssociatedStableIds();
        String associatedString = "";

        if(associatedIds != null && associatedIds.length > 0) {
          associatedString += "&associatedStableIds=";
          for(int i = 0; i < associatedIds.length; i++) {
            associatedString += associatedIds[i] + " "; 
          }
        }

        String[] files = comment.getFiles();
        String fileString = "";
        if(files != null && files.length > 0) {
          
          for(int i = 0; i < files.length; i++) {
            fileString += "&files=" + files[i];
          }
        }

        String strand = "+";
        if(comment.getLocationString().matches("(.*)reverse(.*)")){
          strand = "-";
        }

        String commentPage = "/addComment.do?projectId=" + projectId 
                             + "&stableId=" + stableId 
                             + "&commentTargetId=" + commentTargetId
                             + "&content=" + comment.getContent()
                             + "&locType=" + "genomer"
                             + "&organism=" + comment.getOrganism()
                             + "&locations=" + locString
                             + "&externalDbName=" + edb.getExternalDbName()
                             + "&email=" + email
                             + categoryString
                             + pmIdString
                             + accessionsString
                             + associatedString
                             + fileString
                             + "&strand=" + strand
                             + "&commentId=" + commentId
                             + "&headline=" + comment.getHeadline();

        // redirect to the show comments page
        return new ActionForward(commentPage, true);
    }
}
