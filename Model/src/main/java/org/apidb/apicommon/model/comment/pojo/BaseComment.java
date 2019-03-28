package org.apidb.apicommon.model.comment.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseComment {

  private final Set<String> _relatedStableIds;

  private final Set<String> _digitalObjectIds;

  private final Set<String> _genBankAccessions;

  private final Set<String> _additionalAuthors;

  private final Set<String> _pubMedIds;

  private Target _target;

  private long _userId;

  private String _headline;

  private String _content;

  private String _sequence;

  private ExternalDatabase _externalDatabase;

  private String organism;

  private Location _location;

  public BaseComment() {
    _target = new Target();
    _relatedStableIds = new HashSet<>();
    _digitalObjectIds = new HashSet<>();
    _genBankAccessions = new HashSet<>();
    _additionalAuthors = new HashSet<>();
    _pubMedIds = new HashSet<>();
  }

  public long getUserId() {
    return _userId;
  }

  public BaseComment setUserId(long userId) {
    _userId = userId;
    return this;
  }

  public String getContent() {
    return _content;
  }

  public BaseComment setContent(String content) {
    _content = content;
    return this;
  }

  public String getHeadline() {
    return _headline;
  }

  public BaseComment setHeadline(String headline) {
    _headline = headline;
    return this;
  }

  public Target getTarget() {
    return _target;
  }

  public BaseComment setTarget(Target target) {
    _target = target;
    return this;
  }

  public String getOrganism() {
    return organism;
  }

  public Location getLocation() {
    return _location;
  }

  @JsonIgnore
  public Optional<Location> locationOption() {
    return Optional.ofNullable(_location);
  }

  public BaseComment setLocation(final Location locs) {
    _location = locs;
    return this;
  }

  public BaseComment setOrganism(String organism) {
    this.organism = organism;
    return this;
  }

  public String getSequence() {
    return _sequence;
  }

  public BaseComment setSequence(String sequence) {
    _sequence = sequence;
    return this;
  }

  public ExternalDatabase getExternalDatabase() {
    return _externalDatabase;
  }

  public BaseComment setExternalDatabase(ExternalDatabase externalDatabase) {
    _externalDatabase = externalDatabase;
    return this;
  }

  public Set<String> getRelatedStableIds() {
    return Collections.unmodifiableSet(_relatedStableIds);
  }

  protected BaseComment addRelatedStableId(String id) {
    _relatedStableIds.add(id);
    return this;
  }

  public BaseComment setRelatedStableIds(Collection<String> ids) {
    _relatedStableIds.clear();
    _relatedStableIds.addAll(ids);
    return this;
  }

  public Set<String> getDigitalObjectIds() {
    return Collections.unmodifiableSet(_digitalObjectIds);
  }

  protected BaseComment addDigitalObjectId(String id) {
    _digitalObjectIds.add(id);
    return this;
  }

  public BaseComment setDigitalObjectIds(Collection<String> ids) {
    _digitalObjectIds.clear();
    _digitalObjectIds.addAll(ids);
    return this;
  }

  public Set<String> getGenBankAccessions() {
    return Collections.unmodifiableSet(_genBankAccessions);
  }

  protected BaseComment addGenBankAccession(String id) {
    _genBankAccessions.add(id);
    return this;
  }

  public BaseComment setGenBankAccessions(Collection<String> ids) {
    _genBankAccessions.clear();
    _genBankAccessions.addAll(ids);
    return this;
  }

  public Set<String> getAdditionalAuthors() {
    return Collections.unmodifiableSet(_additionalAuthors);
  }

  protected BaseComment addAdditionalAuthor(String author) {
    _additionalAuthors.add(author);
    return this;
  }

  public BaseComment setAdditionalAuthors(Collection<String> authors) {
    _additionalAuthors.clear();
    _additionalAuthors.addAll(authors);
    return this;
  }

  public Set<String> getPubMedIds() {
    return Collections.unmodifiableSet(_pubMedIds);
  }

  public BaseComment addPubMedId(String id) {
    _pubMedIds.add(id);
    return this;
  }
}
