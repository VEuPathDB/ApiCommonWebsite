package org.apidb.apicommon.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.upload.FormFile;

public class UserFileUploadForm extends ActionForm {

    private FormFile file;
    
    public void setFile(FormFile file) {
        this.file = file;
    }

    public FormFile getFile() {
        return file;
    }
}
