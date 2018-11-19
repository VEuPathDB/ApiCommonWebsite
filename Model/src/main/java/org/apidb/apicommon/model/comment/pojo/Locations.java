package org.apidb.apicommon.model.comment.pojo;

import java.util.*;
import java.util.stream.Collectors;

public class Locations {

  private boolean reversed;

  private final Set<LocationRange> _ranges;

  private String coordinateType;

  public Locations() {
    _ranges = new HashSet<>();
  }

  public Set<LocationRange> getRanges() {
    return Collections.unmodifiableSet(_ranges);
  }

  public Locations setRanges(final Collection<LocationRange> ranges) {
    _ranges.clear();
    _ranges.addAll(ranges);
    return this;
  }

  public Locations addRange(final LocationRange ranges) {
    _ranges.add(ranges);
    return this;
  }

  public boolean isReversed() {
    return reversed;
  }

  public Locations setReversed(boolean reversed) {
    this.reversed = reversed;
    return this;
  }

  public String getCoordinateType() {
    return coordinateType;
  }

  public Locations setCoordinateType(String coordinateType) {
    this.coordinateType = coordinateType;
    return this;
  }

  @Override
  public String toString() {
    if(_ranges.isEmpty())
      return "";

    StringBuilder sb = new StringBuilder();

    sb.append(getCoordinateType())
        .append(": ")
        .append(_ranges.stream()
            .map(Object::toString)
            .collect(Collectors.joining(", ")));

    if (isReversed())
      sb.append(" (reverse strand)");
    else
      sb.append(" (forward strand)");

    return sb.toString();
  }
}
