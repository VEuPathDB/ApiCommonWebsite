package org.apidb.apicommon.controller;

import org.apidb.apicommon.model.UserFileUploadException;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import org.json.JSONException;
import org.xml.sax.SAXException;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.upload.FormFile;

import org.gusdb.wdk.controller.CConstants;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.jspwrap.UserBean;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;

import org.apidb.apicommon.model.UserFile;
import org.apidb.apicommon.model.UserFileFactory;

public class UserFileUploadAction extends Action {

    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        // get the referer link
        String referer = (String) request.getParameter(CConstants.WDK_REFERER_URL_KEY);
        if (referer == null) referer = request.getHeader("referer");

        int index = referer.lastIndexOf("/");
        referer = referer.substring(index);
        ActionForward forward = new ActionForward(referer, false);

        UserFileUploadForm cuForm = (UserFileUploadForm)form;

        FormFile formFile      = cuForm.getFile();
        String contentType = formFile.getContentType();
        String fileName    = formFile.getFileName();
        int fileSize       = formFile.getFileSize();
        byte[] fileData    = formFile.getFileData();

        // user and project metadata code pilfered from 
        // org.apidb.apicommon.controller.ProcessAddCommentAction
        UserBean user = (UserBean) request.getSession().getAttribute(
                CConstants.WDK_USER_KEY);
        if (user == null || user.isGuest()) {
            // This is the case where the session times out while the user is on
            // form page, or someone maliciously trying to post to the
            // form action directly. Return to the form page, where it is
            // handled correctly.
            return forward;
        }
        WdkModelBean wdkModel = (WdkModelBean) getServlet().getServletContext().getAttribute(
                CConstants.WDK_MODEL_KEY);


        String email = user.getEmail().trim().toLowerCase();
        String userUID = user.getSignature().trim();
        String projectName = wdkModel.getDisplayName();
        String projectVersion = wdkModel.getVersion();        

        UserFile userFile = new UserFile(userUID);
        userFile.setFileName(fileName);
        userFile.setFileData(fileData);
        userFile.setContentType(contentType);
        userFile.setFileSize(fileSize);
        userFile.setEmail(email);
        userFile.setUserUID(userUID);
        userFile.setProjectName(projectName);
        userFile.setProjectVersion(projectVersion);

        getUserFileFactory().addUserFile(userFile);
        
        System.out.println("contentType " + userFile.getContentType());
        System.out.println("fileName " + userFile.getFileName());
        System.out.println("fileSize " + userFile.getFileSize());
        System.out.println("owner " + email);
        System.out.println("ownerUID " + userFile.getUserUID());
        System.out.println("projectName " + userFile.getProjectName());
        System.out.println("projectVersion " + userFile.getProjectVersion());
        
        //Set file name to the request object
        request.setAttribute("fileName",fileName);
        request.setAttribute("fileSize",fileSize);

        return new ActionForward("/communityUploadResult.jsp",true);
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
            // the comment factory is not initialized yet, do it
            ServletContext application = getServlet().getServletContext();

            // get the gus_home & project id
            String gusHome = application.getRealPath(application.getInitParameter(Utilities.SYSTEM_PROPERTY_GUS_HOME));
            String projectId = application.getInitParameter(Utilities.ARGUMENT_PROJECT_ID);

            UserFileFactory.initialize(gusHome, projectId);
            factory = UserFileFactory.getInstance();
        }
        return factory;
    }

}

/**
validation:
    Notes <= 4000 chars
    filename <= 255 chars

**/
