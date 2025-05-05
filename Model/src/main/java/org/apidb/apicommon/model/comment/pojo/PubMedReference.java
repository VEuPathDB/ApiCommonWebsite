package org.apidb.apicommon.model.comment.pojo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * PubMed resource details
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PubMedReference {
  private final String _id;
  private final String _title;
  private final String _journal;
  private final String _author;
  private final String _url;
  private final String _status;

  @JsonCreator
  public PubMedReference(
    @JsonProperty("id")      String id,
    @JsonProperty("title")   String title,
    @JsonProperty("journal") String journal,
    @JsonProperty("author")  String author,
    @JsonProperty("url")     String url,
    @JsonProperty("status")  String status
  ) {
    _id = id;
    _title = title;
    _journal = journal;
    _author = author;
    _url = url;
    _status = status;
  }

  /**
   * @return PubMed resource id
   */
  public String getId() {
    return _id;
  }

  /**
   * @return PubMed resource title
   */
  public String getTitle() {
    return _title;
  }

  /**
   * @return PubMed resource journal
   */
  public String getJournal() {
    return _journal;
  }

  /**
   * @return PubMed resource author
   */
  public String getAuthor() {
    return _author;
  }

  /**
   * @return PubMed resource URL
   */
  public String getUrl() {
    return _url;
  }

  public String getStatus() {
    return _status;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    PubMedReference that = (PubMedReference) o;
    return Objects.equals(getId(), that.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId());
  }
}
