package org.apidb.apicommon.model.view;

import java.text.DecimalFormat;

public class Span {

    private static final DecimalFormat FORMAT = new DecimalFormat("#,###");

    private final String sourceId;
    private String sequenceId;
    private int start;
    private int end;
    private boolean forward;
    private float percentStart;
    private float percentLength;
    private String context;

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
    
    public String getStartFormatted() {
        return FORMAT.format(start);
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }
    
    public String getEndFormatted() {
        return FORMAT.format(end);
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

    public void setContext(String context) {
        this.context = context;
    }

    public String getContext() {
        return sequenceId + ":" + context;
    }
}
