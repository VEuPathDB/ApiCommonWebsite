package org.apidb.apicommon.model.comment.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Category {

  private final String name;

  private final int id;

  public Category(int id, String name) {
    this.name = name;
    this.id = id;
  }

  public String getName() {
    return this.name;
  }

  @JsonProperty("value")
  public int getId() {
    return this.id;
  }
}
