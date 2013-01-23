package org.apidb.apicommon.model.view.genome;

import java.util.ArrayList;
import java.util.List;

public class Region {

  private long start;
  private long end;
  private double percentStart;
  private double percentLength;
  private String sequenceId;

  private final List<Feature> features = new ArrayList<>();

  public Region(String sequenceId, long start, long end) {
    this.sequenceId = sequenceId;
    this.start = start;
    this.end = end;
  }

  public String getSourceId() {
    return sequenceId + "-" + start + "-" + end;
  }

  public String toString() {
    return getSourceId();
  }

  public long getStart() {
    return start;
  }

  public void setStart(long start) {
    this.start = start;
  }

  public long getEnd() {
    return end;
  }

  public void setEnd(long end) {
    this.end = end;
  }

  public String getSequenceId() {
    return sequenceId;
  }

  public void setSequenceId(String sequenceId) {
    this.sequenceId = sequenceId;
  }

  public List<Feature> getFeatures() {
    return features;
  }

  public void addFeature(Feature feature) {
    features.add(feature);
  }

  public int getFeatureCount() {
    return features.size();
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

  public int getForwardCount() {
    int count = 0;
    for (Feature feature : features) {
      if (feature.isForward())
        count++;
    }
    return count;
  }

  public int getReverseCount() {
    int count = 0;
    for (Feature feature : features) {
      if (!feature.isForward())
        count++;
    }
    return count;
  }
}
