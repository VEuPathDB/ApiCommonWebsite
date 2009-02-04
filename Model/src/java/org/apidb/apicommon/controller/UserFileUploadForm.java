package org.apidb.apicommon.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.upload.FormFile;
import org.apache.struts.upload.MultipartRequestHandler;

import org.apache.log4j.Logger;

public class UserFileUploadForm extends ActionForm {
    
//    private static int MAX_FILE_SIZE = 
    private Logger logger = Logger.getLogger(UserFileUploadForm.class);
    private FormFile file;
    private String notes;
    
    public void setFile(FormFile file) {
        this.file = file;
    }

    public FormFile getFile() {
        return file;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getNotes() {
        return notes;
    }


/** the mapped.properties strings should go into a properties file?? **/    
    public ActionErrors validate(ActionMapping mapping, 
        HttpServletRequest request) { 
        ActionErrors errors = new ActionErrors();

        Boolean maxLengthExceeded = (Boolean) request.getAttribute(
                    MultipartRequestHandler.ATTRIBUTE_MAX_LENGTH_EXCEEDED);
        if (maxLengthExceeded != null && maxLengthExceeded.booleanValue()) {
            errors.add(ActionErrors.GLOBAL_ERROR, 
            new ActionError("mapped.properties", "file to large (" +
                "SIZEGOESHERE" + ")", "contact us for further instructions")); 
            return errors;
        }

        if (file == null) {
            errors.add(ActionErrors.GLOBAL_ERROR, 
            new ActionError("mapped.properties", "file not found", "select a file for upload")); 
            return errors;
        }
        
        if (file.getFileName() == null || file.getFileName().trim().length() == 0) {
            errors.add(ActionErrors.GLOBAL_ERROR, 
            new ActionError("mapped.properties", "file not found", "select a file for upload")); 
        }

        if (getNotes() == null || getNotes().trim().length() == 0) {
            errors.add(ActionErrors.GLOBAL_ERROR, 
            new ActionError("mapped.properties", "no description", "please add a description")); 
        }
    
        if (getNotes().trim().length() > 4000) {
            errors.add(ActionErrors.GLOBAL_ERROR, 
            new ActionError("mapped.properties", "description is too long (" + 
                getNotes().trim().length() + " characters)", 
                "please add a description no longer than 4000 characters (including spaces)")); 
        }
    
        return errors; 
    }

}
/**
validation:
    Notes <= 4000 chars
    filename <= 255 chars

**/
