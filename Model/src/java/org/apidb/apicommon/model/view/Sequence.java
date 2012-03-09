package org.apidb.apicommon.model.view;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class Sequence {

    private static final DecimalFormat FORMAT = new DecimalFormat("#,###");

    private final String sourceId;
    private final List<Span> spans;
    private int length;
    private float percentLength;
    private String chromosome;

    public Sequence(String sourceId) {
        this.sourceId = sourceId;
        this.spans = new ArrayList<Span>();
    }

    public int getLength() {
        return length;
    }

    public String getLengthFormatted() {
        return FORMAT.format(length);
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void addSpan(Span span) {
        spans.add(span);
    }

    public Span[] getSpans() {
        return spans.toArray(new Span[0]);
    }

    public String getSpanCountFormatted() {
        return FORMAT.format(spans.size());
    }

    public float getPercentLength() {
        return percentLength;
    }

    public void setPercentLength(float percentLength) {
        this.percentLength = percentLength;
    }

    public String getChromosome() {
        return chromosome;
    }

    public void setChromosome(String chromosome) {
        this.chromosome = chromosome;
    }
}
