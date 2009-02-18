/**
 * 
 */
package org.apidb.apicommon.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.gusdb.wdk.model.WdkModelException;

/**
 * @author xingao
 * 
 */
public class Comment {

    public static final String COMMENT_REVIEW_STATUS_UNKNOWN = "unknown";

    private int commentId;
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
     * the site name where the comment is made, eg. "PlasmoDB"
     */
    private String projectName;
    /**
     * the version of the site where the comment is added, eg. '5.0'
     */
    private String projectVersion;
    private String headline;
    private String content;
    /**
     * review status of the comment, determined by the moderator, The newly
     * posted comments are set to "unknown". This field contains the internal
     * value of a controlled vocabulary.
     */
    private String reviewStatus;

    /**
     * The organism name the comment's stable id belongs to
     */
    private String organism;

    private List<Location> locations;

    private List<ExternalDatabase> externalDbs;

    // the user name and organization are filled in later from wdkModel, and
    // they are only for display purpose
    private String userName;
    private String organization;

    private int[] targetCategoryIds;
    private String[] pmIds;
    private String[] accessions;
    private String[] files;
    private String[] associatedStableIds;

    public Comment(String email) {
        this.email = email;
        locations = new ArrayList<Location>();
        externalDbs = new ArrayList<ExternalDatabase>();

        // setup default values
        commentTarget = "gene";
        conceptual = false;
        commentDate = new Date();
        stableId = " ";
        projectName = " ";
        projectVersion = " ";
        headline = " ";
        content = " ";
        reviewStatus = Comment.COMMENT_REVIEW_STATUS_UNKNOWN;
    }

    /**
     * @return Returns the commentId.
     */
    public int getCommentId() {
        return commentId;
    }

    /**
     * @param commentId
     *          The commentId to set.
     */
    void setCommentId(int commentId) {
        this.commentId = commentId;
    }

    /**
     * @return Returns the int[] targetCategoryIds.
     */
    public int[] getTargetCategoryIds() {
        return targetCategoryIds;
    }

    /**
     * @param int[] targetCategoryIds
     *          The targetCategoryIds to set.
     */
    public void setTargetCategoryIds(int[] targetCategoryIds) {
        this.targetCategoryIds = targetCategoryIds;
    }

    /**
     * @return Returns the String[] pmIds.
     */
    public String[] getPmIds() {
        return pmIds;
    }

    /**
     * @param String[] pmIds
     *          The pmIds to set.
     */
    public void setPmIds(String[] pmIds) {
        this.pmIds = pmIds;
    }

    /**
     * @return Returns the String[] accessions.
     */
    public String[] getAccessions() {
        return accessions;
    }

    /**
     * @param String[] accessions
     *          The genbank accessions to set.
     */
    public void setAccessions(String[] accessions) {
        this.accessions = accessions;
    }

    /**
     * @return Returns the String[] files (name).
     */
    public String[] getFiles() {
        return files;
    }

    /**
     * @param String[] files
     *          The file names to set.
     */
    public void setFiles(String[] files) {
        this.files = files;
    }

    /**
     * @return Returns the associatedStableIds[] gene_ids. 
     */
    public String[] getAssociatedStableIds() {
        return associatedStableIds;
    }

    /**
     * @param String[] associatedStableIds
     *          The related gene names to set.
     */
    public void setAssociatedStableIds(String[] associatedStableIds) {
        this.associatedStableIds = associatedStableIds;
    }

    /**
     * @return Returns the commentDate.
     */
    public Date getCommentDate() {
        return commentDate;
    }

    /**
     * @param commentDate
     *          The commentDate to set.
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
     *          The commentTarget to set.
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
     *          The conceptual to set.
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
     *          The content to set.
     */
    public void setContent(String content) {
        this.content = qualify(content);
    }

    /**
     * @return Returns the headline.
     */
    public String getHeadline() {
        if (headline == null || headline.trim().length() == 0) {
            String cnt = content.trim();
            if (cnt.length() <= 40) headline = cnt + " ";
            else {
                int pos = cnt.indexOf(' ', 40);
                if (pos < 0) pos = cnt.length();
                headline = cnt.substring(0, pos) + "...";
            }
        }
        return headline;
    }

    /**
     * @param headline
     *          The headline to set.
     */
    public void setHeadline(String headline) {
        this.headline = qualify(headline);
    }

    /**
     * @return Returns the projectName.
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * @param projectName
     *          The projectName to set.
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * @return Returns the projectVersion.
     */
    public String getProjectVersion() {
        return projectVersion;
    }

    /**
     * @param projectVersion
     *          The projectVersion to set.
     */
    public void setProjectVersion(String projectVersion) {
        this.projectVersion = projectVersion;
    }

    /**
     * @return Returns the reviewStatus.
     */
    public String getReviewStatus() {
        return reviewStatus;
    }

    /**
     * @param reviewStatus
     *          The reviewStatus to set.
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
     *          The stableId to set.
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

    public Location addLocation(boolean reversed, long locationStart,
            long locationEnd, String coordinateType) {
        Location location = new Location(this);
        location.setReversed(reversed);
        location.setLocationStart(locationStart);
        location.setLocationEnd(locationEnd);
        location.setCoordinateType(coordinateType);
        locations.add(location);
        return location;
    }

    public void setLocations(boolean reversed, String locations,
            String coordinateType) throws WdkModelException {
        if (locations == null || locations.length() == 0) return;
        try {
            String[] parts = locations.split(",");
            for (String part : parts) {
                String[] loc = part.trim().split("\\-");
                long start = Long.parseLong(loc[0]);
                long end = Long.parseLong(loc[1]);
                addLocation(reversed, start, end, coordinateType);
            }
        } catch (IndexOutOfBoundsException ex) {
            throw new WdkModelException("Invalid location format: " + locations);
        }
    }

    public Location[] getLocations() {
        Location[] array = new Location[locations.size()];
        locations.toArray(array);
        return array;
    }

    public String getLocationString() {
        if (locations.isEmpty()) return " ";
        StringBuffer sb = new StringBuffer();
        Location location = locations.get(0);
        sb.append(location.getCoordinateType());
        sb.append(": " + location);
        for (int i = 1; i < locations.size(); i++) {
            sb.append(", " + locations.get(i));
        }

        if (location.isReversed()) sb.append(" (reverse strand)");
        else sb.append(" (forward strand)");
        return sb.toString();
    }

    public ExternalDatabase addExternalDatabase(String externalDbName,
            String externalDbVersion) {
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
        if (content == null) content = "";
        // content = content.replaceAll("'", "''");
        return content;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Email:\t" + email + "\n");
        sb.append("CommentTarget:\t" + commentTarget + "\n");
        sb.append("CommentDate:\t" + commentDate + "\n");
        sb.append("StableId:\t" + stableId + "\n");
        sb.append("IsConceptual:\t" + conceptual + "\n");
        sb.append("ProjectName:\t" + projectName + "\n");
        sb.append("ProjectVersion:\t" + projectVersion + "\n");
        sb.append("Headline:\t" + headline + "\n");
        sb.append("ReviewStatus:\t" + reviewStatus + "\n");
        sb.append("Organism:\t" + organism + "\n");
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

    /**
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @param userName
     *          the userName to set
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * @return the organization
     */
    public String getOrganization() {
        return organization;
    }

    /**
     * @param organization
     *          the organization to set
     */
    public void setOrganization(String organization) {
        this.organization = organization;
    }

    /**
     * @return the organism
     */
    public String getOrganism() {
        return organism;
    }

    /**
     * @param organism
     *          the organism to set
     */
    public void setOrganism(String organism) {
        this.organism = organism;
    }
}
