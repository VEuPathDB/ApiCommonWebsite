package org.apidb.apicommon.controller;

import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;

import org.apache.struts.upload.FormFile;
import org.apache.struts.upload.MultipartRequestHandler;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.Globals;

import org.apache.log4j.Logger;

public class UserFileUploadForm extends ActionForm {
    
    private Logger logger = Logger.getLogger(UserFileUploadForm.class);
	private ArrayList formFiles = null; 
    private FormFile file;
    private String notes;
    private String title;
	private int index;

	public UserFileUploadForm() {
		formFiles = new ArrayList();
		index = 0;
	}

    public void setFile(int indx, FormFile file) {
        this.file = file;
        setFormFiles(file);
        index++;
    }
    public FormFile getFile() {
        return file;
    }

    public void setFormFiles(FormFile file) {
        this.formFiles.add(index, file);
    }
	public ArrayList getFormFiles() {
        return formFiles;
	}
    
    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
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

        ModuleConfig mc = (ModuleConfig) request.getAttribute(Globals.MODULE_KEY);
        String maxFileSize = mc.getControllerConfig().getMaxFileSize();
        Boolean maxLengthExceeded = (Boolean) request.getAttribute(
                    MultipartRequestHandler.ATTRIBUTE_MAX_LENGTH_EXCEEDED);
        if (maxLengthExceeded != null && maxLengthExceeded.booleanValue()) {
            errors.add(ActionErrors.GLOBAL_ERROR, 
            new ActionError("mapped.properties", "file upload is larger than the allowed " +
                maxFileSize, "(total for all files) contact us for further instructions")); 
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

        if (getTitle() == null || getTitle().trim().length() == 0) {
            errors.add(ActionErrors.GLOBAL_ERROR, 
            new ActionError("mapped.properties", "no title", "please add a title")); 
        }
    
        if (getTitle().trim().length() > 4000) {
            errors.add(ActionErrors.GLOBAL_ERROR, 
            new ActionError("mapped.properties", "title is too long (" + 
                getTitle().trim().length() + " characters)", 
                "please add a title no longer than 4000 characters (including spaces)")); 
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

	public void reset(ActionMapping mapping, HttpServletRequest request) {
		file = null;
	}

}
/**
validation:
    Notes <= 4000 chars
    filename <= 255 chars

**/
