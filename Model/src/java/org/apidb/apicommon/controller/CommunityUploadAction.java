package org.apidb.apicommon.controller;

import  org.apidb.apicommon.controller.CommunityUploadForm;

import java.io.*;

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
import org.gusdb.wdk.model.jspwrap.UserBean;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;

public class CommunityUploadAction extends Action {

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

        CommunityUploadForm cuForm = (CommunityUploadForm)form;

        FormFile file      = cuForm.getFile();
        String contentType = file.getContentType();
        String fileName    = file.getFileName();
        int fileSize       = file.getFileSize();
        byte[] fileData    = file.getFileData();

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

        System.out.println("contentType " + contentType);
        System.out.println("fileName " + fileName);
        System.out.println("fileSize " + fileSize);
        System.out.println("owner " + email);
        System.out.println("ownerUID " + userUID);
        System.out.println("projectName " + projectName);
        System.out.println("projectVersion " + projectVersion);
        
        String filePath = getServlet().getServletContext().getRealPath("/") +"upload";

        if(!fileName.equals("")){  
            System.out.println("Server path:" +filePath);
            //Create file
            File fileOnDisk = new File(filePath, fileName);
            //If file does not exists create file                      
            if(!fileOnDisk.exists()){
              FileOutputStream fileOutStream = new FileOutputStream(fileOnDisk);
              fileOutStream.write(file.getFileData());
              fileOutStream.flush();
              fileOutStream.close();
            }  
    
    
        }
        //Set file name to the request object
        request.setAttribute("fileName",fileName);
        request.setAttribute("fileSize",fileSize);

        return new ActionForward("/communityUploadResult.jsp",true);
    }
}

/**
validation:
    Notes <= 4000 chars
    filename <= 255 chars

**/