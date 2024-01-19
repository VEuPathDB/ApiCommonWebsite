package org.apidb.apicommon.service.services.jbrowse.model;

public enum VDIDatasetType {
  RNA_SEQ("RNA-Seq"),
  BIGWIG("Bigwig");

  private String vdiName;

  VDIDatasetType(String vdiName) {
    this.vdiName = vdiName;
  }

  public String getVdiName() {
    return vdiName;
  }
}
