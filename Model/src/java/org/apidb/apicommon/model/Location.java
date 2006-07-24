/**
 * 
 */
package org.apidb.apicommon.model;

/**
 * @author xingao
 * 
 */
public class Location {

    private Comment comment;
    private boolean reversed;
    private long locationStart;
    private long locationEnd;
    private String coordinateType;

    public Location(Comment comment) {
        this.comment = comment;
    }

    /**
     * @return Returns the locationEnd.
     */
    public long getLocationEnd() {
        return locationEnd;
    }

    /**
     * @param locationEnd
     *            The locationEnd to set.
     */
    public void setLocationEnd(long locationEnd) {
        this.locationEnd = locationEnd;
    }

    /**
     * @return Returns the locationStart.
     */
    public long getLocationStart() {
        return locationStart;
    }

    /**
     * @param locationStart
     *            The locationStart to set.
     */
    public void setLocationStart(long locationStart) {
        this.locationStart = locationStart;
    }

    /**
     * @return Returns the reversed.
     */
    public boolean isReversed() {
        return reversed;
    }

    /**
     * @param reversed
     *            The reversed to set.
     */
    public void setReversed(boolean reversed) {
        this.reversed = reversed;
    }

    /**
     * @return Returns the comment.
     */
    public Comment getComment() {
        return comment;
    }

    /**
     * @return Returns the coordinateType.
     */
    public String getCoordinateType() {
        return coordinateType;
    }

    /**
     * @param coordinateType
     *            The coordinateType to set.
     */
    public void setCoordinateType(String coordinateType) {
        this.coordinateType = coordinateType;
    }
}
