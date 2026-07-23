package org.apidb.apicommon.model.comment.pojo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * POJO for a row in the {@code comment_ai_run} sidecar table — the shared,
 * immutable LLM-output cache keyed by the content-digest {@code job_id}.
 *
 * <p>See {@code Service/CLAUDE-ai-user-comments.md} (DB schema). One row exists
 * per distinct (gene, synonyms, source, model, promptVersion, options) digest;
 * it is the durable source of truth that satisfies cache hits after the
 * in-memory job registry entry has been evicted. {@code terminalStatus} is one
 * of {@code success | mentioned-in-passing | gene-not-mentioned}.
 */
public class CommentAiRun {

  private String _jobId;                 // hex SHA-256, primary key
  private String _modelName;
  private String _promptVersion;
  private AiRunSource _source;           // pubmed or upload, a sealed union
  private String _geneId;
  private final List<String> _synonymsUsed = new ArrayList<>();
  private String _optionsJson;           // canonical JSON of request `options`
  private JobStatus _terminalStatus;     // one of the three publishable terminals
  private boolean _onlyMentionedInPassing;
  private String _aiHeadline;            // null unless terminalStatus == 'success'
  private String _aiContent;             // null unless terminalStatus == 'success'
  private Date _completedAt;

  public String getJobId() { return _jobId; }
  public CommentAiRun setJobId(String jobId) { _jobId = jobId; return this; }

  public String getModelName() { return _modelName; }
  public CommentAiRun setModelName(String modelName) { _modelName = modelName; return this; }

  public String getPromptVersion() { return _promptVersion; }
  public CommentAiRun setPromptVersion(String promptVersion) { _promptVersion = promptVersion; return this; }

  public AiRunSource getSource() { return _source; }
  public CommentAiRun setSource(AiRunSource source) { _source = source; return this; }

  public String getGeneId() { return _geneId; }
  public CommentAiRun setGeneId(String geneId) { _geneId = geneId; return this; }

  public List<String> getSynonymsUsed() { return Collections.unmodifiableList(_synonymsUsed); }
  public CommentAiRun setSynonymsUsed(Collection<String> synonyms) {
    _synonymsUsed.clear();
    _synonymsUsed.addAll(synonyms);
    return this;
  }

  public String getOptionsJson() { return _optionsJson; }
  public CommentAiRun setOptionsJson(String optionsJson) { _optionsJson = optionsJson; return this; }

  public JobStatus getTerminalStatus() { return _terminalStatus; }
  public CommentAiRun setTerminalStatus(JobStatus terminalStatus) { _terminalStatus = terminalStatus; return this; }

  public boolean isOnlyMentionedInPassing() { return _onlyMentionedInPassing; }
  public CommentAiRun setOnlyMentionedInPassing(boolean v) { _onlyMentionedInPassing = v; return this; }

  public String getAiHeadline() { return _aiHeadline; }
  public CommentAiRun setAiHeadline(String aiHeadline) { _aiHeadline = aiHeadline; return this; }

  public String getAiContent() { return _aiContent; }
  public CommentAiRun setAiContent(String aiContent) { _aiContent = aiContent; return this; }

  public Date getCompletedAt() { return _completedAt; }
  public CommentAiRun setCompletedAt(Date completedAt) { _completedAt = completedAt; return this; }
}
