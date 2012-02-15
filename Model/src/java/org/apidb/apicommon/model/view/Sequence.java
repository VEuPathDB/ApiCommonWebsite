package org.apidb.apicommon.model.view;

import java.util.ArrayList;
import java.util.List;

public class Sequence {

    private final String sourceId;
    private final List<Span> dynamicSpans;
    private int length;
    private float percentLength;
    
    public Sequence(String sourceId) {
        this.sourceId = sourceId;
        this.dynamicSpans = new ArrayList<Span>();
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getSourceId() {
        return sourceId;
    }
    
    public void addSpan(Span span) {
        dynamicSpans.add(span);
    }
    
    public Span[] getSpans() {
        return dynamicSpans.toArray(new Span[0]);
    }

    public float getPercentLength() {
        return percentLength;
    }

    public void setPercentLength(float percentLength) {
        this.percentLength = percentLength;
    }
}
