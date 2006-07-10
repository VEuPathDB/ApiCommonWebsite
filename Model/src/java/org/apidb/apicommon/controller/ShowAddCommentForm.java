/**
 * 
 */
package org.apidb.apicommon.controller;

import org.apache.struts.action.ActionForm;
import org.apidb.apicommon.model.CommentTarget;

/**
 * @author xingao
 * 
 */
public class ShowAddCommentForm extends ActionForm {

    /**
     * 
     */
    private static final long serialVersionUID = 3058638510150202538L;

    private CommentTarget commentTarget;
    private String stableId;
    private String refererUrl;

    /**
     * @return Returns the commentTarget.
     */
    public CommentTarget getCommentTarget() {
        return commentTarget;
    }

    /**
     * @param commentTarget The commentTarget to set.
     */
    public void setCommentTarget(CommentTarget commentTarget) {
        this.commentTarget = commentTarget;
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
}
