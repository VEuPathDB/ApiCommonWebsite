package org.apidb.apicommon.service.services.jbrowse.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 */
public class JBrowseTrack {
  // Fields with defaults
  private String storeClass = "JBrowse/Store/SeqFeature/BigWig";
  private String yScalePosition = "left";
  private String type = "JBrowse/View/Track/Wiggle/XYPlot";
  private String category = "My Data from Galaxy";

  @JsonProperty("min_score")
  private int minScore = 0;
  @JsonProperty("max_score")
  private int maxScore = 1000;


  // Fields without defaults
  private String key;
  private String label;
  private String urlTemplate;
  private Metadata metadata;
  private Style style;
  private String subcategory;

  // No Args constructor for jackson
  public JBrowseTrack() {

  }

  public String getStoreClass() {
    return storeClass;
  }

  public void setStoreClass(String storeClass) {
    this.storeClass = storeClass;
  }

  public String getyScalePosition() {
    return yScalePosition;
  }

  public void setyScalePosition(String yScalePosition) {
    this.yScalePosition = yScalePosition;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getUrlTemplate() {
    return urlTemplate;
  }

  public void setUrlTemplate(String urlTemplate) {
    this.urlTemplate = urlTemplate;
  }

  public Metadata getMetadata() {
    return metadata;
  }

  public void setMetadata(Metadata metadata) {
    this.metadata = metadata;
  }

  public Style getStyle() {
    return style;
  }

  public void setStyle(Style style) {
    this.style = style;
  }

  public int getMinScore() {
    return minScore;
  }

  public void setMinScore(int minScore) {
    this.minScore = minScore;
  }

  public int getMaxScore() {
    return maxScore;
  }

  public void setMaxScore(int maxScore) {
    this.maxScore = maxScore;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public void setSubcategory(String subcategory) {
    this.subcategory = subcategory;
  }

  public String getSubcategory() {
    return subcategory;
  }

  public static class Style {
    @JsonProperty("pos_color")
    private String posColor = "#5B2C6F";

    public Style() {
    }

    public String getPosColor() {
      return posColor;
    }

    public void setPosColor(String posColor) {
      this.posColor = posColor;
    }
  }

  public static class Metadata {
    private String trackType = "Coverage";

    private String subcategory;
    private String dataset;
    private String mdescription;

    public Metadata() {
    }

    public String getTrackType() {
      return trackType;
    }

    public void setTrackType(String trackType) {
      this.trackType = trackType;
    }

    public String getSubcategory() {
      return subcategory;
    }

    public void setSubcategory(String subcategory) {
      this.subcategory = subcategory;
    }

    public String getDataset() {
      return dataset;
    }

    public void setDataset(String dataset) {
      this.dataset = dataset;
    }

    public String getMdescription() {
      return mdescription;
    }

    public void setMdescription(String mdescription) {
      this.mdescription = mdescription;
    }
  }
}
