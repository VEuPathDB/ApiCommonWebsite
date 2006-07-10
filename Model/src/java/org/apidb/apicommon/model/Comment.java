/**
 * 
 */
package org.apidb.apicommon.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author xingao
 * 
 */
public class Comment {

    private String email;
    /**
     * the internal key of the controlled vocabulary: gene, protein, genome
     */
    private String commentTarget;
    private Date commentDate;
    /**
     * the source id (gene_id) or accession_id (sequence id)
     */
    private String stableId;
    /**
     * whether it's conceptual comment or not; determined by the moderator. The
     * newly posted comments are not conceptual
     */
    private boolean conceptual;
    /**
     * the site id where the comment is made, eg. "plasmodb 5.0"
     */
    private String projectId;
    private String headline;
    private String content;
    /**
     * review status of the comment, determined by the moderator, The newly
     * posted comments are set to "unknown". This field contains the internal
     * value of a controlled vocabulary.
     */
    private String reviewStatus;
    
    private List<Location> locations;
    
    private List<ExternalDatabase> externalDbs;

    public Comment(String email) {
        this.email = email;
        locations = new ArrayList<Location>();
        externalDbs = new ArrayList<ExternalDatabase>();
        
        // setup default values
        commentTarget = " ";
        conceptual = false;
        commentDate = new Date();
        stableId = " ";
        projectId = " ";
        headline = " ";
        content = " ";
        reviewStatus = " ";
    }

    /**
     * @return Returns the commentDate.
     */
    public Date getCommentDate() {
        return commentDate;
    }

    /**
     * @param commentDate
     *            The commentDate to set.
     */
    public void setCommentDate(Date commentDate) {
        this.commentDate = commentDate;
    }

    /**
     * @return Returns the commentTarget.
     */
    public String getCommentTarget() {
        return commentTarget;
    }

    /**
     * @param commentTarget
     *            The commentTarget to set.
     */
    public void setCommentTarget(String commentTarget) {
        this.commentTarget = qualify(commentTarget);
    }

    /**
     * @return Returns the conceptual.
     */
    public boolean isConceptual() {
        return conceptual;
    }

    /**
     * @param conceptual
     *            The conceptual to set.
     */
    public void setConceptual(boolean conceptual) {
        this.conceptual = conceptual;
    }

    /**
     * @return Returns the content.
     */
    public String getContent() {
        return content;
    }

    /**
     * @param content
     *            The content to set.
     */
    public void setContent(String content) {
        this.content = qualify(content);
    }

    /**
     * @return Returns the headline.
     */
    public String getHeadline() {
        return headline;
    }

    /**
     * @param headline
     *            The headline to set.
     */
    public void setHeadline(String headline) {
        this.headline = qualify(headline);
    }

    /**
     * @return Returns the projectId.
     */
    public String getProjectId() {
        return projectId;
    }

    /**
     * @param projectId
     *            The projectId to set.
     */
    public void setProjectId(String projectId) {
        this.projectId = qualify(projectId);
    }

    /**
     * @return Returns the reviewStatus.
     */
    public String getReviewStatus() {
        return reviewStatus;
    }

    /**
     * @param reviewStatus
     *            The reviewStatus to set.
     */
    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = qualify(reviewStatus);
    }

    /**
     * @return Returns the stableId.
     */
    public String getStableId() {
        return stableId;
    }

    /**
     * @param stableId
     *            The stableId to set.
     */
    public void setStableId(String stableId) {
        this.stableId = qualify(stableId);
    }

    /**
     * @return Returns the email.
     */
    public String getEmail() {
        return email;
    }
    
    public Location addLocation(boolean reversed, long locationStart, long locationEnd) {
        Location location= new Location(this);
        location.setReversed(reversed);
        location.setLocationStart(locationStart);
        location.setLocationEnd(locationEnd);
        locations.add(location);
        return location;
    }
    
    public void setLocations(boolean reversed, String locations) {
        if (locations == null ||locations.length() == 0) return;
        String[] parts = locations.split(",");
        for (String part : parts) {
            String[] loc = part.trim().split("\\-");
            long start = Long.parseLong(loc[0]);
            long end = Long.parseLong(loc[1]);
            Location location = new Location(this);
            location.setReversed(reversed);
            location.setLocationStart(start);
            location.setLocationEnd(end);
            this.locations.add(location);
        }
    }
    
    public Location[] getLocations() {
        Location[] array = new Location[locations.size()];
        locations.toArray(array);
        return array;
    }
    
    public ExternalDatabase addExternalDatabase(String externalDbName, String externalDbVersion) {
        ExternalDatabase externalDb = new ExternalDatabase();
        externalDb.setExternalDbName(externalDbName);
        externalDb.setExternalDbVersion(externalDbVersion);
        externalDbs.add(externalDb);
        return externalDb;
    }
    
    public ExternalDatabase[] getExternalDbs() {
        ExternalDatabase[] array = new ExternalDatabase[externalDbs.size()];
        externalDbs.toArray(array);
        return array;
    }
    
    private String qualify(String content) {
        // replace all single quotes with two single quotes
        content = content.replaceAll("'", "''");
        return content;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Email:\t" + email + "\n");
        sb.append("CommentTarget:\t" + commentTarget+ "\n");
        sb.append("CommentDate:\t" + commentDate+ "\n");
        sb.append("StableId:\t" + stableId+ "\n");
        sb.append("IsConceptual:\t" + conceptual+ "\n");
        sb.append("ProjectId:\t" + projectId+ "\n");
        sb.append("Headline:\t" + headline+ "\n");
        sb.append("ReviewStatus:\t" + reviewStatus+ "\n");
        sb.append("Locations:\t");
        for (Location loc : locations) {
            sb.append(loc.getLocationStart() + "-" + loc.getLocationEnd());
            if (loc.isReversed()) sb.append("[REV]");
            sb.append(", ");
        }
        sb.append("\n");
        sb.append("ExternalDatabases:\t");
        for (ExternalDatabase edb : externalDbs) {
            sb.append(edb.getExternalDbName());
            sb.append("(" + edb.getExternalDbVersion() + "), ");
        }
        sb.append("\n");
        sb.append("Content:\t" + content + "\n\n");
        return sb.toString();
    }
}
