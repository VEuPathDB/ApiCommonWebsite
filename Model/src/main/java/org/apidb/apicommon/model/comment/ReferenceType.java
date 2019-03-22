package org.apidb.apicommon.model.comment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Optional;

public enum ReferenceType {
  DIGITAL_OBJECT_ID("doi"),
  ACCESSION("genbank"),
  AUTHOR("author"),
  PUB_MED("pubmed");

  public final String dbName;

  ReferenceType(String dbName) {
    this.dbName = dbName;
  }

  @Override
  @JsonValue
  public String toString() {
    return dbName;
  }

  @JsonCreator
  public static Optional<ReferenceType> fromDbName(String name) {
    for(ReferenceType t : values())
      if(t.dbName.equals(name))
        return Optional.of(t);

    return Optional.empty();
  }
}
