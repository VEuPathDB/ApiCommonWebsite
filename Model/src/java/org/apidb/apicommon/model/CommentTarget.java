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
    private boolean requireLocation;

    public CommentTarget(String internalValue) {
        this.internalValue = internalValue;
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
     * @return Returns the internalValue.
     */
    public String getInternalValue() {
        return internalValue;
    }

    /**
     * @return Returns the requireLocation.
     */
    public boolean isRequireLocation() {
        return requireLocation;
    }

    /**
     * @return Returns the requireLocation.
     */
    public boolean getRequireLocation() {
        return requireLocation;
    }

    /**
     * @param requireLocation
     *            The requireLocation to set.
     */
    public void setRequireLocation(boolean requireLocation) {
        this.requireLocation = requireLocation;
    }
}
