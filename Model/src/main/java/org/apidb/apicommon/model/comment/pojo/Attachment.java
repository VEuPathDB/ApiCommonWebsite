package org.apidb.apicommon.model.comment.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apidb.apicommon.model.userfile.UserFile;

import java.util.Objects;

/**
 * User Comment File Attachment
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Attachment {
  private long _id;
  private String _name;
  private String _description;
  private String _preview;

  public long getId() {
    return _id;
  }

  public Attachment setId(long id) {
    _id = id;
    return this;
  }

  public String getName() {
    return _name;
  }

  public Attachment setName(String name) {
    _name = name;
    return this;
  }

  public String getDescription() {
    return _description;
  }

  public Attachment setDescription(String description) {
    _description = description;
    return this;
  }

  public String getPreview() {
    return _preview;
  }

  public void setPreview(String preview) {
    _preview = preview;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Attachment that = (Attachment) o;
    return getId() == that.getId();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId());
  }

  public static Attachment fromUserFile(UserFile uf) {
    return new Attachment().setId(uf.getUserFileId())
        .setName(uf.getFileName())
        .setDescription(uf.getNotes());
  }
}
