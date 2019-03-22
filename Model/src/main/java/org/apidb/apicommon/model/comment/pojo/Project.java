package org.apidb.apicommon.model.comment.pojo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Details about the site where a comment was made.
 */
public class Project {
  /**
   * Project name.  E.g. "PlasmoDB"
   */
  private final String _name;

  /**
   * Project version.  E.g. "5.0"
   */
  private final String _version;

  @JsonCreator
  public Project(
    @JsonProperty("name")    String name,
    @JsonProperty("version") String version
  ) {
    _name = name;
    _version = version;
  }

  public String getName() {
    return _name;
  }

  public String getVersion() {
    return _version;
  }
}
