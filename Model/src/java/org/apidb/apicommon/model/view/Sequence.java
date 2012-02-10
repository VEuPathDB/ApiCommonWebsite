package org.apidb.apicommon.model.view;

import java.util.ArrayList;
import java.util.List;

public class Sequence {

    private final String sourceId;
    private final List<DynamicSpan> dynamicSpans;
    private int length;
    
    public Sequence(String sourceId) {
        this.sourceId = sourceId;
        this.dynamicSpans = new ArrayList<DynamicSpan>();
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
    
    public void addSpan(DynamicSpan span) {
        dynamicSpans.add(span);
    }
    
    public DynamicSpan[] getSpans() {
        return dynamicSpans.toArray(new DynamicSpan[0]);
    }
}
