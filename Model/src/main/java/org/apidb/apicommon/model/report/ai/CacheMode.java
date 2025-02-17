package org.apidb.apicommon.model.report.ai;

public enum CacheMode {

  TEST("test"),
  POPULATE("populate");

  private final String mode;

  CacheMode(String mode) {
    this.mode = mode;
  }

  public String getMode() {
    return mode;
  }

  public static CacheMode fromString(String mode) throws IllegalArgumentException {
    for (CacheMode cm : CacheMode.values()) {
      if (cm.mode.equalsIgnoreCase(mode)) {
	return cm;
      }
    }
    throw new IllegalArgumentException("Invalid CacheMode: " + mode);
  }
}
