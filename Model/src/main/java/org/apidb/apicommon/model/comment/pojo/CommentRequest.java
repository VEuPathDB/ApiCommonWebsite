package org.apidb.apicommon.model.comment.pojo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * POJO representing a request for a the creation of a new comment.
 */
public class CommentRequest extends BaseComment{

  /**
   * Previous comment id.
   *
   * Comments are not updated.  For some reason "updating" a comment creates
   * a new record and hides the old one.  If this is present, this request
   * represents a new comment request which is intended to replace the old
   * comment.
   */
  private Long previousCommentId;

  /**
   * Comment categories
   */
  private Set<Integer> _categoryIds;

  /**
   *
   */
  private Locations _locations;

  @JsonCreator
  public CommentRequest(
      @JsonProperty("userId") long userId,
      @JsonProperty("title")  String title
  ) {
    super(userId);
    setHeadline(Objects.requireNonNull(title));
  }

  public Set<Integer> getCategoryIds() {
    return Collections.unmodifiableSet(_categoryIds);
  }

  public CommentRequest setCategoryIds(Collection<Integer> categoryIds) {
    _categoryIds.clear();
    _categoryIds.addAll(categoryIds);
    return this;
  }

  public Locations getLocations() {
    return _locations;
  }

  public CommentRequest setLocations(Locations locations) {
    _locations = locations;
    return this;
  }

  public Long getPreviousCommentId() {
    return previousCommentId;
  }

  public CommentRequest setPreviousCommentId(Long previousCommentId) {
    this.previousCommentId = previousCommentId;
    return this;
  }

  @Override
  public CommentRequest setOrganism(String organism) {
    return (CommentRequest) super.setOrganism(organism);
  }

  @Override
  public CommentRequest setExternalDb(ExternalDatabase externalDb) {
    return (CommentRequest) super.setExternalDb(externalDb);
  }

  @Override
  public CommentRequest setAdditionalAuthors(Collection<String> authors) {
    return (CommentRequest) super.setAdditionalAuthors(authors);
  }

  @Override
  public CommentRequest setSequence(String sequence) {
    return (CommentRequest) super.setSequence(sequence);
  }

  @Override
  public CommentRequest setGenBankAccessions(Collection<String> ids) {
    return (CommentRequest) super.setGenBankAccessions(ids);
  }

  @Override
  public CommentRequest setDigitalObjectIds(Collection<String> ids) {
    return (CommentRequest) super.setDigitalObjectIds(ids);
  }

  @Override
  public CommentRequest setHeadline(String headline) {
    return (CommentRequest) super.setHeadline(headline);
  }

  @Override
  public CommentRequest setContent(String content) {
    return (CommentRequest) super.setContent(content);
  }

  @Override
  public CommentRequest setRelatedStableIds(Collection<String> ids) {
    return (CommentRequest) super.setRelatedStableIds(ids);
  }
}
