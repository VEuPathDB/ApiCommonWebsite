package org.apidb.apicommon.model.comment.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
class BaseComment {
  private final long _userId;

  private final Target _target;

  private final Set<String> _relatedStableIds;

  private final Set<String> _digitalObjectIds;

  private final Set<String> _genBankAccessions;

  private final Set<String> _additionalAuthors;

  private final Set<String> _pubMedIds;

  private String _headline;

  private String _content;

  private String _sequence;

  private ExternalDatabase _externalDb;

  private String organism;

  public BaseComment(long userId) {
    _target = new Target();
    _relatedStableIds = new HashSet<>();
    _digitalObjectIds = new HashSet<>();
    _genBankAccessions = new HashSet<>();
    _additionalAuthors = new HashSet<>();
    _pubMedIds = new HashSet<>();
    _userId = userId;
  }

  public long getUserId() {
    return _userId;
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

  public String getOrganism() {
    return organism;
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

  public ExternalDatabase getExternalDb() {
    return _externalDb;
  }

  public BaseComment setExternalDb(ExternalDatabase externalDb) {
    _externalDb = externalDb;
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

  public BaseComment setPubMedIds(Collection<String> ids) {
    _pubMedIds.clear();
    _pubMedIds.addAll(ids);
    return this;
  }
}
