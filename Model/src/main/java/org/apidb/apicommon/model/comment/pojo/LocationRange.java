package org.apidb.apicommon.model.comment.pojo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class LocationRange {

  private final long start;
  private final long end;

  @JsonCreator
  public LocationRange(
    @JsonProperty("start") long start,
    @JsonProperty("end")   long end
  ) {
    this.start = start;
    this.end = end;
  }

  public long getStart() {
    return start;
  }

  public long getEnd() {
    return end;
  }

  @Override
  public String toString() {
    return String.format("%d-%d", start, end);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    LocationRange that = (LocationRange) o;
    return getStart() == that.getStart() && getEnd() == that.getEnd();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getStart(), getEnd());
  }
}
