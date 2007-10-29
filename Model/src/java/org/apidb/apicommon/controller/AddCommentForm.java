/**
 * 
 */
package org.apidb.apicommon.controller;

import org.apache.struts.action.ActionForm;

/**
 * @author xingao
 * 
 */
public class AddCommentForm extends ActionForm {

    /**
     * 
     */
    private static final long serialVersionUID = -3512734487544282555L;

    private String headline;
    private String content;
    private String commentTarget;
    private String stableId;
    private String reversed;
    private String locations;
    private String refererUrl;
    private String organism;

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
        return locations;
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
}
