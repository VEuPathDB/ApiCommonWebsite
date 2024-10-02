package org.apidb.apicommon.service.services.jbrowse.model;

import java.util.Arrays;

public enum VDIDatasetType {
  RNA_SEQ("RNASeq", "RNASeq"),
  BIGWIG("BigwigFiles", "Bigwig Files From User");

  private String vdiName;
  private String jbrowseSubcategoryName;

  VDIDatasetType(String vdiName, String jbrowseSubcategoryName) {
    this.vdiName = vdiName;
    this.jbrowseSubcategoryName = jbrowseSubcategoryName;
  }

  public String getVdiName() {
    return vdiName;
  }

  public String getJbrowseSubcategoryName() {
    return jbrowseSubcategoryName;
  }

  public static VDIDatasetType fromVDIName(String vdiName) {
    return Arrays.stream(values())
        .filter(val -> val.vdiName.equalsIgnoreCase(vdiName))
        .findFirst()
        .orElseThrow();
  }
}
