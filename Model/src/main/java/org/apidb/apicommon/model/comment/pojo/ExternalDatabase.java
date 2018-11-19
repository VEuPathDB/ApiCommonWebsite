package org.apidb.apicommon.model.comment.pojo;

public class ExternalDatabase {

  private String _name;
  private String _version;

  public ExternalDatabase() {}

  public ExternalDatabase(String name, String version) {
    _name = name;
    _version = version;
  }

  public String getName() {
    return _name;
  }

  public ExternalDatabase setName(String name) {
    _name = name;
    return this;
  }

  public String getVersion() {
    return _version;
  }

  public ExternalDatabase setVersion(String version) {
    _version = version;
    return this;
  }
}
