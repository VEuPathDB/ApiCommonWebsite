package org.apidb.apicommon.model.view;

public class Span {

    private final String sourceId;
    private String sequenceId;
    private int start;
    private int end;
    private boolean forward;
    private float percentStart;
    private float percentLength;

    public Span(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getSequenceId() {
        return sequenceId;
    }

    public void setSequenceId(String sequenceId) {
        this.sequenceId = sequenceId;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public String getSourceId() {
        return sourceId;
    }

    public boolean isForward() {
        return forward;
    }

    public void setForward(boolean forward) {
        this.forward = forward;
    }

    public float getPercentStart() {
        return percentStart;
    }

    public void setPercentStart(float percentStart) {
        this.percentStart = percentStart;
    }

    public float getPercentLength() {
        return percentLength;
    }

    public void setPercentLength(float percentLength) {
        this.percentLength = percentLength;
    }
}
