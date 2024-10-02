package org.apidb.apicommon.service.services.jbrowse.model;

import java.util.List;

public class JBrowseDatasetResponse {
  private List<JBrowseTrack> tracks;

  public JBrowseDatasetResponse() {

  }

  public void setTracks(List<JBrowseTrack> tracks) {
    this.tracks = tracks;
  }

  public List<JBrowseTrack> getTracks() {
    return tracks;
  }
}
