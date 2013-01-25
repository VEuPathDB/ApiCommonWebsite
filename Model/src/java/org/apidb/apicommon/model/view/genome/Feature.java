package org.apidb.apicommon.model.view.genome;

import java.text.DecimalFormat;

public class Feature {

    private static final DecimalFormat FORMAT = new DecimalFormat("#,###");

    private final String sourceId;
    private String sequenceId;
    private long start;
    private long end;
    private boolean forward;
    private double percentStart;
    private double percentLength;
    private String context;
    private String description;

    public Feature(String sourceId) {
        this.sourceId = sourceId;
    }

    public Feature(Feature feature) {
      this.context = feature.context;
      this.end = feature.end;
      this.forward = feature.forward;
      this.percentLength = feature.percentLength;
      this.percentStart = feature.percentStart;
      this.sequenceId = feature.sequenceId;
      this.sourceId = feature.sourceId;
      this.start = feature.start;
      this.description = feature.description;
    }

    public String getSequenceId() {
        return sequenceId;
    }

    public void setSequenceId(String sequenceId) {
        this.sequenceId = sequenceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getStart() {
        return start;
    }
    
    public String getStartFormatted() {
        return FORMAT.format(start);
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }
    
    public String getEndFormatted() {
        return FORMAT.format(end);
    }

    public void setEnd(long end) {
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

    public double getPercentStart() {
        return percentStart;
    }

    public void setPercentStart(double percentStart) {
        this.percentStart = percentStart;
    }

    public double getPercentLength() {
        return percentLength;
    }

    public void setPercentLength(double percentLength) {
        this.percentLength = percentLength;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getContext() {
        return sequenceId + ":" + context;
    }
}
