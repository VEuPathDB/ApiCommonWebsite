package org.apidb.apicommon.jmx.mbeans.wdk;

public class Meta extends BeanBase implements MetaMBean   {
  
  public Meta() {
    super();
  }

  public String getModelVersion() {
    return wdkModel.getVersion();
  }
  
  public String getDisplayName() {
    return wdkModel.getDisplayName();
  }
  
  public String getIntroduction() {
    return wdkModel.getIntroduction();
  }

  public String getProjectId() {
      return wdkModel.getProjectId();
  }

  public String getName() {
      return wdkModel.getProjectId();
  }

  public String getReleaseDate() {
      return wdkModel.getReleaseDate();
  }

}
