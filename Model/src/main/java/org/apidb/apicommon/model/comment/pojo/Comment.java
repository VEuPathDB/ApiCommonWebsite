package org.apidb.apicommon.model.comment.pojo;

import com.fasterxml.jackson.annotation.*;
import org.apidb.apicommon.model.comment.ReviewStatus;

import java.util.*;

public class Comment extends BaseComment {

  private final long _commentId;

  private Date _commentDate;

  private final Set<String> _categories;

  private final Set<PubMedReference> _pubMed;

  private final Set<Attachment> _attachments;

  private boolean _conceptual;

  private ReviewStatus _reviewStatus;

  private Locations _locations;

  private Project _project;

  /**
   * Comment author
   */
  private Author _author;

  @JsonCreator
  public Comment(
    @JsonProperty("id")     long comId,
    @JsonProperty("userId") long userId
  ) {
    super(userId);
    _commentId = comId;

    _attachments = new HashSet<>();
    _categories = new HashSet<>();
    _pubMed = new HashSet<>();

    // setup default values
    _conceptual = false;
    _commentDate = new Date();
    _reviewStatus = ReviewStatus.UNKNOWN;
  }

  public long getId() {
    return _commentId;
  }

  public Set<String> getCategories() {
    return Collections.unmodifiableSet(_categories);
  }

  public Comment setCategories(Collection<String> categoryNames) {
    _categories.clear();
    _categories.addAll(categoryNames);
    return this;
  }

  @JsonGetter("pubMed")
  public Collection<PubMedReference> getPubMedRefs() {
    return Collections.unmodifiableCollection(_pubMed);
  }

  @JsonIgnore
  public Comment addPubMedRef(PubMedReference ref) {
    _pubMed.add(ref);
    return this;
  }

  @JsonSetter("pubMed")
  public Comment setPubMedRefs(Collection<PubMedReference> pmRefs) {
    _pubMed.clear();
    _pubMed.addAll(pmRefs);
    return this;
  }

  @Override
  @JsonIgnore
  public Set<String> getPubMedIds() {
    return super.getPubMedIds();
  }

  public Date getCommentDate() {
    return _commentDate;
  }

  public Comment setCommentDate(Date commentDate) {
    this._commentDate = commentDate;
    return this;
  }

  public boolean isConceptual() {
    return _conceptual;
  }

  public Comment setConceptual(boolean conceptual) {
    this._conceptual = conceptual;
    return this;
  }

  public Project getProject() {
    return _project;
  }

  public Comment setProject(Project project) {
    _project = project;
    return this;
  }

  public ReviewStatus getReviewStatus() {
    return _reviewStatus;
  }

  public Comment setReviewStatus(ReviewStatus reviewStatus) {
    this._reviewStatus = reviewStatus;
    return this;
  }

  public Locations getLocations() {
    return _locations;
  }

  public Comment setLocations(final Locations locs) {
    _locations = locs;
    return this;
  }

  public Author getAuthor() {
    return _author;
  }

  public Comment setAuthor(final Author author) {
    _author = author;
    return this;
  }

  public Collection<Attachment> getAttachments() {
    return Collections.unmodifiableCollection(_attachments);
  }

  public Comment setAttachments(Collection<Attachment> files) {
    _attachments.clear();
    files.stream().peek(Objects::requireNonNull).forEach(_attachments::add);
    return this;
  }

  @Override
  public Comment setOrganism(String organism) {
    return (Comment) super.setOrganism(organism);
  }

  @Override
  public Comment addDigitalObjectId(String id) {
    return (Comment) super.addDigitalObjectId(id);
  }

  @Override
  public Comment setDigitalObjectIds(Collection<String> ids) {
    return (Comment) super.setDigitalObjectIds(ids);
  }

  @Override
  public Comment setContent(String content) {
    return (Comment) super.setContent(content);
  }

  @Override
  public Comment setHeadline(String headline) {
    return (Comment) super.setHeadline(headline);
  }

  @Override
  public Comment setSequence(String sequence) {
    return (Comment) super.setSequence(sequence);
  }

  @Override
  public Comment setRelatedStableIds(Collection<String> ids) {
    return (Comment) super.setRelatedStableIds(ids);
  }

  @Override
  public Comment addRelatedStableId(String id) {
    return (Comment) super.addRelatedStableId(id);
  }

  @Override
  public Comment setAdditionalAuthors(Collection<String> authors) {
    return (Comment) super.setAdditionalAuthors(authors);
  }

  @Override
  public Comment setExternalDb(ExternalDatabase externalDb) {
    return (Comment) super.setExternalDb(externalDb);
  }

  @Override
  public Comment addGenBankAccession(String id) {
    return (Comment) super.addGenBankAccession(id);
  }
  @Override
  public Comment setGenBankAccessions(Collection<String> ids) {
    return (Comment) super.setGenBankAccessions(ids);
  }

  @Override
  public Comment addAdditionalAuthor(String author) {
    return (Comment) super.addAdditionalAuthor(author);
  }
}
