package org.apidb.apicommon.model.comment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ReviewStatus {
  ACCEPTED("accepted"),
  ADOPTED("adopted"),
  COMMUNITY("community"),
  NOT_SPAM("not_spam"),
  REJECTED("rejected"),
  SPAM("spam"),
  TASK("task"),
  UNKNOWN("unknown");

  public final String dbName;

  ReviewStatus(String dbName) {
    this.dbName = dbName;
  }

  @Override
  @JsonValue
  public String toString() {
    return dbName;
  }

  @JsonCreator
  public static ReviewStatus fromString(String value) {
    for(final ReviewStatus stat : values())
      if(stat.dbName.equals(value))
        return stat;
    throw new IllegalArgumentException("invalid review status value " + value);
  }
}
