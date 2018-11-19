package org.apidb.apicommon.model.comment.pojo;

/**
 * Comment Target Object
 */
public class Target {
  private String _type;

  private String _id;

  public String getType() {
    return _type;
  }

  public Target setType(String type) {
    this._type = type;
    return this;
  }

  public String getId() {
    return _id;
  }

  public Target setId(String id) {
    this._id = id;
    return this;
  }
}
