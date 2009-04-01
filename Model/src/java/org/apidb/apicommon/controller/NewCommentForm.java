package org.apidb.apicommon.controller;

import org.apache.struts.action.ActionMapping;
import javax.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Iterator;

import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;

import org.apache.struts.action.ActionForm;

import org.apache.struts.upload.FormFile;
import org.apache.struts.upload.MultipartRequestHandler;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.Globals;

public class NewCommentForm extends ActionForm {

    private String headline;
    private String content;
    private String commentTarget;
    private String stableId;
    private String reversed;
    private String locations;
    private String refererUrl;
    private String organism;

    private HashMap<Integer, FormFile> formFiles = null;
    private HashMap<Integer, String> formNotes = null;
    private FormFile file;
    private String notes;

    private String commentTargetId;
    private String externalDbName;
    private String externalDbVersion;
    private String locType;
    private String[] targetCategory;
    private String pmIds;
    private String accessions;
    private String associatedStableIds;
    private String contig;

    public NewCommentForm() {
      formFiles = new HashMap();
      formNotes = new HashMap();
    }

    /**
     * @return Returns the content.
     */
    public String getContent() {
        return content;
    }

    /**
     * @param content The content to set.
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * @return Returns the headline.
     */
    public String getHeadline() {
        return headline;
    }

    /**
     * @param headline The headline to set.
     */
    public void setHeadline(String headline) {
        this.headline = headline;
    }

    /**
     * @return Returns the stableId.
     */
    public String getStableId() {
        return stableId;
    }

    /**
     * @param stableId The stableId to set.
     */
    public void setStableId(String stableId) {
        this.stableId = stableId;
    }

    /**
     * @return Returns the commentTarget.
     */
    public String getCommentTarget() {
        return commentTarget;
    }

    /**
     * @param commentTarget The commentTarget to set.
     */
    public void setCommentTarget(String commentTarget) {
        this.commentTarget = commentTarget;
    }

    /**
     * @return Returns the locations.
     */
    public String getLocations() {
        return this.locations;
    }

    /**
     * @param locations The locations to set.
     */
    public void setLocations(String locations) {
        this.locations = locations;
    }

    /**
     * @return Returns the reversed.
     */
    public String getReversed() {
        return reversed;
    }

    /**
     * @param reversed The reversed to set.
     */
    public void setReversed(String reversed) {
        this.reversed = reversed;
    }

    /**
     * @return Returns the refererUrl.
     */
    public String getRefererUrl() {
        return refererUrl;
    }

    /**
     * @param refererUrl The refererUrl to set.
     */
    public void setRefererUrl(String refererUrl) {
        this.refererUrl = refererUrl;
    }

    /**
     * @return the organism
     */
    public String getOrganism() {
        return organism;
    }

    /**
     * @param organism the organism to set
     */
    public void setOrganism(String organism) {
        this.organism = organism;
    }

    public void setCommentTargetId(String id) {
      this.commentTargetId = id;
    }

    public String getCommentTargetId() {
      return this.commentTargetId;
    }

    public void setExternalDbName(String id) {
      this.externalDbName = id;
    }

    public String getExternalDbName() {
      return this.externalDbName;
    }

    public void setExternalDbVersion(String id) {
      this.externalDbVersion = id;
    }

    public String getExternalDbVersion() {
      return this.externalDbVersion;
    }

    public void setLocType(String id) {
      this.locType = id;
    }

    public String getLocType() {
      return this.locType;
    }

    public void setTargetCategory(String[] id) {
      this.targetCategory = id;
    }

    public String[] getTargetCategory() {
      return this.targetCategory;
    }

    public void setPmIds(String id) {
      this.pmIds = id;
    }

    public String getPmIds() {
      return this.pmIds;
    }

    public void setContig(String contig) {
      this.contig = contig;
    }

    public String getContig() {
      return this.contig;
    }


    public void setAccessions(String id) {
      this.accessions = id;
    }

    public String getAccessions() {
      return this.accessions;
    }

    public void setAssociatedStableIds(String id) {
      this.associatedStableIds = id;
    }

    public String getAssociatedStableIds() {
      return this.associatedStableIds;
    }

    public void setFile(int indx, FormFile file) {
      this.file = file;
      setFormFiles(indx, file);
    }

    public FormFile getFile() {
      return file;
    }

    public void setFormFiles(int indx, FormFile file) {
        this.formFiles.put(indx, file);
    }

    public HashMap getFormFiles() {
      return formFiles;
    } 

    public void setNotes(int indx, String notes) {
      this.notes = notes;
      setFormNotes(indx, notes);
    }

    public String getNotes() {
      return notes;
    }

    public void setFormNotes(int indx, String notes) {
        this.formNotes.put(indx, notes);
    }

    public HashMap getFormNotes() {
      return formNotes;
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

      if ((getStableId() == null) || (getStableId().trim().equals(""))) {
        errors.add(ActionErrors.GLOBAL_ERROR,
          new ActionError("mapped.properties", "No gene name ", "This is probably due to browse session reset. Please go back to that gene page and start it again!"));
        return errors;
			}

      if ((getHeadline() == null) || (getHeadline().trim().equals(""))) {
        errors.add(ActionErrors.GLOBAL_ERROR,
          new ActionError("mapped.properties", "No headline ", "headline is required!"));
        return errors;
      }

      if ((getContent() == null) || (getContent().trim().equals(""))) {
        errors.add(ActionErrors.GLOBAL_ERROR,
          new ActionError("mapped.properties", "No content ", "content is required!"));
        return errors;
      }

      Iterator it = formFiles.keySet().iterator();
      while (it.hasNext()) {
        Integer i = (Integer) it.next();
        if (formFiles.get(i).getFileName().trim().length() == 0 &&
            formNotes.get(i).trim().length() == 0) {
          it.remove(); 
          formNotes.remove(i); 
        } 
      }

      for (Integer i : formFiles.keySet()) {
        if (formFiles.get(i) == null) {
          errors.add(ActionErrors.GLOBAL_ERROR,
                   new ActionError("mapped.properties", "File not found", "select a file for upload"));
          return errors;
        }

        if (formFiles.get(i).getFileName() == null || formFiles.get(i).getFileName().trim().length() == 0) { 
          errors.add(ActionErrors.GLOBAL_ERROR, 
          new ActionError("mapped.properties", "File not found", "select a file for upload")); 
        } 
      }

      for (Integer i : formNotes.keySet()) {
        if (formFiles.get(i) == null) continue;            
        if (formNotes.get(i) == null || formNotes.get(i).trim().length() == 0) {
          errors.add(ActionErrors.GLOBAL_ERROR,                
            new ActionError("mapped.properties", "No description", "please add a description"));            
        }

        if (formNotes.get(i) != null && formNotes.get(i).trim().length() > 4000) { 
          errors.add(ActionErrors.GLOBAL_ERROR,                
             new ActionError("mapped.properties", "description is too long (" + formNotes.get(i).trim().length() + " characters)",                    
             "please add a description no longer than 4000 characters (including spaces)"));  
        } 
      }

      return errors; 
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
      file = null;
      formFiles.clear();
      formNotes.clear();

      headline = null;
      content = null;
      commentTarget = null;
      reversed = null;
      locations = null;

      locType = null;
      targetCategory =null ;
      pmIds =null;
      accessions =null;
      associatedStableIds =null;
    }
}
