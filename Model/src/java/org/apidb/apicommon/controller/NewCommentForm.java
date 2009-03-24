package org.apidb.apicommon.controller;

import org.apache.struts.action.ActionMapping;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.util.ArrayList;
import java.io.Serializable;

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

    private ArrayList<FormFile> formFiles = null;
    private FormFile file;
    private int index;

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
      formFiles = new ArrayList<FormFile>();
      index = 0;
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
      setFormFiles(file);
      index++;
    }

    public FormFile getFile() {
      return file;
    }

    public void setFormFiles(FormFile file) {
        this.formFiles.add(index, file);
    }

    public ArrayList<FormFile> getFormFiles() {
      return formFiles;
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

      if ((getHeadline() == null) || (getHeadline().trim().equals(""))) {
        errors.add(ActionErrors.GLOBAL_ERROR,
          new ActionError("mapped.properties", "no headline ", "no headline !!!!"));
        return errors;
      }

      if ((getContent() == null) || (getContent().trim().equals(""))) {
        errors.add(ActionErrors.GLOBAL_ERROR,
          new ActionError("mapped.properties", "no content ", "no content !!!!"));
        return errors;
      }

      return errors; 
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
      file = null;
      formFiles.clear();
      index = 0;

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
