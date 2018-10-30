package org.apidb.apicommon.model.report.summaryview.genome;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONWriter;

public class Sequence implements Comparable<Sequence> {

  private final String sourceId;
  private final List<Region> regions;
  private final List<Feature> features;
  private long length;
  private double percentLength;
  private String chromosome;
  private String organism;
  
  /**
   * serialize this region to a json writer
   * @param writer
   */
  protected void writeJson(JSONWriter writer) {
    writer.object();
    writer.key("sourceId").value(sourceId);
    writer.key("percentLength").value(percentLength);
    writer.key("length").value(length);
    writer.key("chromosome").value(chromosome);
    writer.key("organism").value(organism);
    writer.key("features").array();
    for (Feature feature : features) {
      feature.writeJson(writer);
    }
    writer.endArray();
    writer.key("regions").array();
    for (Region region : regions) {
      region.writeJson(writer);
    }
    writer.endArray();
    writer.endObject();
  }

  public Sequence(String sourceId) {
    this.sourceId = sourceId;
    this.regions = new ArrayList<>();
    this.features = new ArrayList<>();
  }

  public long getLength() {
    return length;
  }

  public String getLengthFormatted() {
    return GenomeViewReporter.FORMAT.format(length);
  }

  public void setLength(long length) {
    this.length = length;
  }

  public String getSourceId() {
    return sourceId;
  }

  public void addRegion(Region region) {
    regions.add(region);
  }

  public int getRegionCount() {
    return regions.size();
  }

  public List<Region> getRegions() {
    return regions;
  }

  public List<Region> getRegions(long start, long end) {
    List<Region> list = new ArrayList<>();
    for (Region region : regions) {
      if (start <= region.getEnd() && end >= region.getStart())
        list.add(region);
    }
    return list;
  }

  public void addFeature(Feature feature) {
    features.add(feature);
  }

  public List<Feature> getFeatures() {
    return features;
  }

  public int getFeatureCount() {
    return features.size();
  }

  public String getFeatureCountFormatted() {
    return GenomeViewReporter.FORMAT.format(getFeatureCount());
  }

  public double getPercentLength() {
    return percentLength;
  }

  public void setPercentLength(double percentLength) {
    this.percentLength = percentLength;
  }

  public String getChromosome() {
    return chromosome;
  }

  public void setChromosome(String chromosome) {
    this.chromosome = chromosome;
  }

  public String getOrganism() {
    return organism;
  }

  public void setOrganism(String organism) {
    this.organism = organism;
  }

  @Override
  public int compareTo(Sequence sequence) {
    long diff = this.features.size() - sequence.features.size();
    if (diff != 0)
      return (diff > 0) ? -1 : 1;
    else
      return sourceId.compareTo(sequence.sourceId);
  }

}
