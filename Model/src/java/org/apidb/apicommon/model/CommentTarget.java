/**
 * 
 */
package org.apidb.apicommon.model;

/**
 * @author xingao
 * 
 */
public class CommentTarget {

    private String internalValue;
    private String displayName;
    private boolean allowLocation;
    private boolean allowReverse;
    private String locationDescription;

    public CommentTarget(String internalValue) {
        this.internalValue = internalValue;
    }

    /**
     * @return Returns the allowLocation.
     */
    public boolean isAllowLocation() {
        return allowLocation;
    }

    /**
     * @param allowLocation
     *            The allowLocation to set.
     */
    public void setAllowLocation(boolean allowLocation) {
        this.allowLocation = allowLocation;
    }

    /**
     * @return Returns the allowReverse.
     */
    public boolean isAllowReverse() {
        return allowReverse;
    }

    /**
     * @param allowReverse
     *            The allowReverse to set.
     */
    public void setAllowReverse(boolean allowReverse) {
        this.allowReverse = allowReverse;
    }

    /**
     * @return Returns the displayName.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @param displayName
     *            The displayName to set.
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * @return Returns the locationDescription.
     */
    public String getLocationDescription() {
        return locationDescription;
    }

    /**
     * @param locationDescription
     *            The locationDescription to set.
     */
    public void setLocationDescription(String locationDescription) {
        this.locationDescription = locationDescription;
    }

    /**
     * @return Returns the internalValue.
     */
    public String getInternalValue() {
        return internalValue;
    }
}
