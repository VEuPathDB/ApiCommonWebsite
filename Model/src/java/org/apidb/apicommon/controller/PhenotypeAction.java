package org.apidb.apicommon.controller;

import java.io.IOException;
import java.util.HashMap;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.upload.FormFile;
import org.apidb.apicommon.model.Comment;
import org.apidb.apicommon.model.UserFile;
import org.apidb.apicommon.model.UserFileFactory;
import org.gusdb.wdk.controller.CConstants;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.jspwrap.UserBean;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;
import org.json.JSONException;
import org.xml.sax.SAXException;

public class PhenotypeAction extends CommentAction {

    private PhenotypeForm cuForm;

    public ActionForward execute(ActionMapping mapping, 
                                 ActionForm form, 
                                 HttpServletRequest request, 
                                 HttpServletResponse response) throws Exception {

        String referer = (String) request.getParameter(CConstants.WDK_REFERER_URL_KEY);
        if (referer == null) referer = request.getHeader("referer");

        int index = referer.lastIndexOf("/");
        referer = referer.substring(index);

        ActionForward forward = new ActionForward(referer, false);
        // forward.setRedirect(true);

        WdkModelBean wdkModel = (WdkModelBean) getServlet().getServletContext().getAttribute(
                CConstants.WDK_MODEL_KEY);
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

        PhenotypeForm cuForm = (PhenotypeForm)form; 

        String email = user.getEmail().trim().toLowerCase();
        int userId = user.getUserId();
        String projectName = wdkModel.getDisplayName();
        String projectVersion = wdkModel.getVersion();
        String stableId = cuForm.getStableId();
        String commentTarget = cuForm.getCommentTargetId();
        String organism = cuForm.getOrganism();
        String headline = cuForm.getHeadline().trim();
        String extDbName = cuForm.getExternalDbName();
        String extDbVersion = cuForm.getExternalDbVersion();
        String content = cuForm.getPhenotypeDescription();
        String pmIdStr = cuForm.getPmIds();
        String mutantStatus = cuForm.getMutantStatus();
        String background = cuForm.getBackground();
        String mutationType = cuForm.getMutationType();
        String mutationMethod = cuForm.getMutationMethod();
        String mutationMethodDescription = cuForm.getMutationMethodDescription();
        String[] markers = (String[])cuForm.getMarker();
        String[] reporters = (String[])cuForm.getReporter();

        String phenotypeCategory = cuForm.getPhenotypeCategory();
        String expression = cuForm.getExpression();

        StringBuffer body = new StringBuffer();
        body.append("Headline: " + headline + "\n");
        body.append("Target: " + commentTarget + "\n");
        body.append("Source_Id: " + stableId + "\n");
        body.append("Comment: " + content + "\n");
        body.append("PMID: " + pmIdStr + "\n");
        body.append("Email: " + email + "\n");
        body.append("Organism: " + organism + "\n");
        body.append("DB Name: " + extDbName + "\n");
        body.append("DB Version: " + extDbVersion + "\n");

        // create a comment instance
        Comment comment = new Comment(email);
        comment.setCommentTarget(commentTarget);
        comment.setStableId(stableId);
        comment.setProjectName(projectName);
        comment.setProjectVersion(projectVersion);
        comment.setHeadline(headline);
        comment.setOrganism(organism);
        comment.setContent(content);  // phenoteyp description
        comment.setBackground(background);  
        comment.setMutantStatus(Integer.parseInt(mutantStatus));  
        comment.setMutationType(Integer.parseInt(mutationType));  
        comment.setMutationMethod(Integer.parseInt(mutationMethod));  
        comment.setUserId(userId);

        comment.addExternalDatabase(extDbName, extDbVersion);

        if((pmIdStr != null) && (pmIdStr.trim().length() != 0)) {
          String[] pmIds = pmIdStr.replaceAll(",", " ").split(" ");
          comment.setPmIds(pmIds);
        }

        if((markers != null) && (markers.length > 0)) {
          int[] markersArray = new int[markers.length];
          for(int i=0; i < markers.length; i++) {
             markersArray[i] = Integer.valueOf(markers[i]).intValue();
          }
          comment.setMutantMarkers(markersArray);
        }

        if((reporters != null) && (reporters.length > 0)) {
          int[] reportersArray = new int[reporters.length];
          for(int i=0; i < reporters.length; i++) {
             reportersArray[i] = Integer.valueOf(reporters[i]).intValue();
          }
          comment.setMutantReporters(reportersArray);
        } 

        HashMap<Integer, Object> formSet = cuForm.getFormFiles();
        HashMap<Integer, String> noteSet = cuForm.getFormNotes();

        String userUID = user.getSignature().trim();

        ArrayList<String> files = new ArrayList<String>();

        for(Integer i : formSet.keySet()) {

            FormFile formFile = (FormFile) formSet.get(i); 

            if((formFile.getFileName() == null) || (formFile.getFileName().length() == 0)) continue; 

            String notes = noteSet.get(i).trim(); 
            String contentType = formFile.getContentType();
            String fileName   = formFile.getFileName();
            int fileSize       = formFile.getFileSize();
            byte[] fileData    = formFile.getFileData();

            UserFile userFile = new UserFile(userUID); 
            userFile.setFileName(fileName); 
            userFile.setFileData(fileData);
            userFile.setContentType(contentType);
            userFile.setFileSize(fileSize);
            userFile.setEmail(email);
            userFile.setUserUID(userUID);
            userFile.setTitle(headline);
            userFile.setNotes(notes);
            userFile.setProjectName(projectName);
            userFile.setProjectVersion(projectVersion);

            getUserFileFactory().addUserFile(userFile);

            int fileId = userFile.getUserFileId();
            String fileStr = fileId + "|" + fileName + "|" + notes;

            files.add(fileStr);
        }

        if(files.size() > 0) {

          String[] f = new String[files.size()];
          comment.setFiles(files.toArray(f));
        } 

        // add the comment

        getCommentFactory().addComment(comment);

        request.setAttribute("submitStatus", "success");
        request.setAttribute("subject", headline);
        request.setAttribute("body", body.toString());

        forward = new ActionForward("/addPhenotype.jsp", false);
        return forward;
    }

    protected UserFileFactory getUserFileFactory() throws WdkModelException,
              NoSuchAlgorithmException, ParserConfigurationException,
              TransformerFactoryConfigurationError, TransformerException,
              IOException, SAXException, SQLException, JSONException,
              WdkUserException, InstantiationException, IllegalAccessException,
              ClassNotFoundException {
        UserFileFactory factory = null;
        try {
           factory = UserFileFactory.getInstance();
        } catch (WdkModelException ex) {
           ServletContext application = getServlet().getServletContext(); 
           String gusHome = application.getRealPath(application.getInitParameter(Utilities.SYSTEM_PROPERTY_GUS_HOME));
         String projectId = application.getInitParameter(Utilities.ARGUMENT_PROJECT_ID);
         UserFileFactory.initialize(gusHome, projectId); 
         factory = UserFileFactory.getInstance(); 
        } 
        return factory; 
    }
}
