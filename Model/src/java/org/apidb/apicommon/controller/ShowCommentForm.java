package org.apidb.apicommon.controller;

import org.apache.struts.action.ActionForm;

public class ShowCommentForm extends ActionForm {

    private int commentId;
    private String stableId;
    private String projectId;

    public int getCommentId() {
        return commentId;
    }

    public void setCommentId(int commentId) {
        this.commentId = commentId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    /**
     * @return the stableId
     */
    public String getStableId() {
        return stableId;
    }

    /**
     * @param stableId
     *          the stableId to set
     */
    public void setStableId(String stableId) {
        this.stableId = stableId;
    }
}
