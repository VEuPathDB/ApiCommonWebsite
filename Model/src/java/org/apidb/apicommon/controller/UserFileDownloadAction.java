package org.apidb.apicommon.controller;

import java.io.File;
import java.io.InputStream;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.apidb.apicommon.controller.DownloadAction;
import org.apidb.apicommon.model.CommentConfig;
import org.apidb.apicommon.model.CommentFactory;
import org.gusdb.wdk.model.Utilities;

public class UserFileDownloadAction extends DownloadAction {

    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest   request, HttpServletResponse   response)
            throws Exception   {

        StreamInfo info = getStreamInfo(mapping, form, request, response);
        
        if (info == null)
            return mapping.findForward("fileNotFound");

        String   contentType = info.getContentType();
        InputStream   stream = info.getInputStream();

        try {
            response.setContentType(contentType);
            copy(stream, response.getOutputStream());
        } finally {
            if (stream != null) {
                stream.close();
            }
        }

        // Tell Struts that we are done with the response.
        return null;
    }

    protected StreamInfo getStreamInfo(
           ActionMapping mapping, 
           ActionForm form,
           HttpServletRequest request, 
           HttpServletResponse response) throws Exception {
        
        ServletContext application = getServlet().getServletContext();
        
        String gusHome = application.getRealPath(application.getInitParameter(Utilities.SYSTEM_PROPERTY_GUS_HOME));
        String projectId = application.getInitParameter(Utilities.ARGUMENT_PROJECT_ID);

        CommentFactory.initialize(gusHome, projectId);
        CommentFactory factory = CommentFactory.getInstance();
        CommentConfig commentConfig = factory.getCommentConfig();
        
        String uploadPath = commentConfig.getUserFileUploadDir();

        String fname = (String) request.getParameter("fname");
        
        String filePath = uploadPath + "/" + projectId + "/" + fname;
        
        File file = new File(filePath);
        
        if (!file.exists()) {
            return null;
        } else {   
            /** Note: content-disposition is broken in Internet Explorer 5.5 
                Service Pack 1 (SP1). See
                http://support.microsoft.com/kb/q279667/
            **/
            response.setHeader("Content-disposition", 
                               "attachment; filename=" + file.getName());        
            String contentType = "application/octet-stream";
            return new FileStreamInfo(contentType, file);
        }
    }

}
