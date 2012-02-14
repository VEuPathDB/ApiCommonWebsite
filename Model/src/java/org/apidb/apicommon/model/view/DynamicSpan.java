package org.apidb.apicommon.model.view;

public class DynamicSpan {

    private final String sourceId;
    private String sequenceId;
    private int start;
    private int end;
    private boolean forward;

    public DynamicSpan(String sourceId) {
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
}
