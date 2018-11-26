package org.apidb.apicommon.model.comment.pojo;

import java.util.*;

/**
 * POJO representing a request for a the creation of a new comment.
 */
public class CommentRequest extends BaseComment {

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

  public CommentRequest() {
    _categoryIds = new HashSet<>();
  }

  public Set<Integer> getCategoryIds() {
    return Collections.unmodifiableSet(_categoryIds);
  }

  public CommentRequest setCategoryIds(Collection<Integer> categoryIds) {
    _categoryIds.clear();
    _categoryIds.addAll(categoryIds);
    return this;
  }

  public CommentRequest setLocation(Location location) {
    return (CommentRequest) super.setLocation(location);
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
  public CommentRequest setExternalDatabase(ExternalDatabase externalDatabase) {
    return (CommentRequest) super.setExternalDatabase(externalDatabase);
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
