package org.apidb.apicommon.model.comment;

import static org.apidb.apicommon.model.comment.ReferenceType.ACCESSION;
import static org.apidb.apicommon.model.comment.ReferenceType.AUTHOR;
import static org.apidb.apicommon.model.comment.ReferenceType.DIGITAL_OBJECT_ID;
import static org.apidb.apicommon.model.comment.ReferenceType.PUB_MED;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.apidb.apicommon.model.comment.pojo.Attachment;
import org.apidb.apicommon.model.comment.pojo.Author;
import org.apidb.apicommon.model.comment.pojo.Category;
import org.apidb.apicommon.model.comment.pojo.Comment;
import org.apidb.apicommon.model.comment.pojo.CommentRequest;
import org.apidb.apicommon.model.comment.pojo.ExternalDatabase;
import org.apidb.apicommon.model.comment.pojo.Project;
import org.apidb.apicommon.model.comment.pojo.PubMedReference;
import org.apidb.apicommon.model.comment.repo.DeleteAttachmentQuery;
import org.apidb.apicommon.model.comment.repo.FindCommentQuery;
import org.apidb.apicommon.model.comment.repo.GetAllAttachmentsQuery;
import org.apidb.apicommon.model.comment.repo.GetAttachmentQuery;
import org.apidb.apicommon.model.comment.repo.GetAuthorQuery;
import org.apidb.apicommon.model.comment.repo.GetCategoriesQuery;
import org.apidb.apicommon.model.comment.repo.GetCommentExistsQuery;
import org.apidb.apicommon.model.comment.repo.GetCommentQuery;
import org.apidb.apicommon.model.comment.repo.GetExternalDatabaseQuery;
import org.apidb.apicommon.model.comment.repo.HideCommentQuery;
import org.apidb.apicommon.model.comment.repo.InsertAttachmentQuery;
import org.apidb.apicommon.model.comment.repo.InsertAuthorQuery;
import org.apidb.apicommon.model.comment.repo.InsertCategoryQuery;
import org.apidb.apicommon.model.comment.repo.InsertCommentQuery;
import org.apidb.apicommon.model.comment.repo.InsertExternalDatabaseLinkQuery;
import org.apidb.apicommon.model.comment.repo.InsertExternalDatabaseQuery;
import org.apidb.apicommon.model.comment.repo.InsertLocationQuery;
import org.apidb.apicommon.model.comment.repo.InsertReferencesQuery;
import org.apidb.apicommon.model.comment.repo.InsertSequenceQuery;
import org.apidb.apicommon.model.comment.repo.InsertStableIdQuery;
import org.apidb.apicommon.model.comment.repo.Table;
import org.apidb.apicommon.model.comment.repo.UpdateAttachmentQuery;
import org.apidb.apicommon.model.comment.repo.UpdateAuthorQuery;
import org.eupathdb.sitesearch.data.comments.UserCommentUpdater;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.runtime.InstanceManager;
import org.gusdb.fgputil.runtime.Manageable;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.config.ModelConfigUserDB;
import org.gusdb.wdk.model.user.User;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Manages user comments on WDK records
 *
 * @author xingao
 */
public class CommentFactory implements Manageable<CommentFactory> {
  private static final ObjectMapper JSON = new ObjectMapper();

  private Project _project;

  private DatabaseInstance _commentDb;
  private DataSource _commentDs;
  private CommentConfig _config;
  private boolean _isReusingUserDb;
  private String _host;
  private WdkModel _wdkModel;

  @Override
  public CommentFactory getInstance(String projectId, String gusHome)
      throws Exception {

    // parse and load the configuration
    CommentConfigParser parser = new CommentConfigParser(gusHome);
    boolean isReusingUserDb;
    try {
      CommentConfig config = parser.parseConfig(projectId);

      // create a platform object
      ModelConfigUserDB userDbConfig = WdkModel.getModelConfig(projectId,
          gusHome).getUserDB();
      DatabaseInstance db;
      if (userDbConfig.isSameConnectionInfoAs(config)) {
        // if connections are the same, then ignore comment config connection
        // info and use UserDB database instance
        db = DatabaseInstance.getAllInstances().get(WdkModel.DB_INSTANCE_USER);
        isReusingUserDb = true;
      }
      else {
        db = new DatabaseInstance(config, "Comment");
        isReusingUserDb = false;
      }

      // create a factory instance
      CommentFactory factory = new CommentFactory();
      // find user factory to use to get users
      WdkModel wdkModel = InstanceManager.getInstance(WdkModel.class, gusHome,
          projectId);

      final String host = wdkModel.getProperties().get("LOCALHOST");

      if (host == null) {
        throw new WdkModelException("model.prop must contain the 'LOCALHOST' property");
      }

      factory.initialize(wdkModel, db, config, isReusingUserDb,
          new Project(projectId, wdkModel.getVersion()), host.endsWith("/")
              ? host.substring(0, host.lastIndexOf("/")) : host);

      return factory;
    }
    catch (CommentModelException ex) {
      throw new WdkModelException();
    }
  }

  public Collection<Category> getCategoriesByType(final String type) throws WdkModelException {
    try (final Connection con = _commentDs.getConnection()) {
      return new GetCategoriesQuery(_config.getCommentSchema())
        .filterByType(type)
        .run(con)
        .value();
    } catch (SQLException e) {
      throw new WdkModelException(e);
    }
  }

  public CommentConfig getCommentConfig() {
    return _config;
  }

  public DataSource getCommentDataSource() {
    return _commentDs;
  }

  public Optional<Comment> getComment(long commentId) throws WdkModelException {
    try(Connection con = _commentDs.getConnection()) {
      final Optional<Comment> out = new GetCommentQuery(
        _config.getCommentSchema(), commentId).run(con).value();
      if (out.isPresent()) {
        final Comment tmp = out.get();
        tmp.setPubMedRefs(getPubMedRefs(tmp));
        attachAiProvenance(con, Collections.singletonList(tmp));
      }

      return out;
    } catch (IOException | SQLException ex) {
      throw new WdkModelException(ex);
    }
  }

  /**
   * Look up the shared AI-run cache row by its content-digest job id. A hit
   * lets the sync prelude short-circuit a submit straight to a terminal
   * cache-hit response (see the AI gene-publication service).
   */
  public Optional<CommentAiRun> findAiRun(String jobId) throws WdkModelException {
    try (Connection con = _commentDs.getConnection()) {
      return new GetCommentAiRunQuery(_config.getCommentSchema(), jobId).run(con).value();
    } catch (SQLException ex) {
      throw new WdkModelException(ex);
    }
  }

  /**
   * Read the raw {@code comment_ai_provenance} row (including the
   * {@code run_job_id} FK) for a comment, or empty if it has none. Distinct from
   * the batched, display-oriented {@link GetCommentAiProvenanceQuery} used by
   * {@link #attachAiProvenance}, which yields an {@code AiProvenanceView} without
   * the FK. Used by the edit flow to carry provenance forward onto a replacement
   * comment so it re-links to the same shared run.
   */
  public Optional<AiProvenance> getAiProvenanceRow(long commentId) throws WdkModelException {
    try (Connection con = _commentDs.getConnection()) {
      return new GetAiProvenanceRowQuery(_config.getCommentSchema(), commentId).run(con).value();
    } catch (SQLException ex) {
      throw new WdkModelException(ex);
    }
  }

  /**
   * Persist the shared AI-run cache row produced by the pipeline's persisting
   * stage, keyed by the content-digest job id. Written at most once per distinct
   * job (the digest dedupes resubmissions). This is the pipeline's only write —
   * the user-comment row is created later by the publish endpoint on approval.
   */
  public void persistAiRun(CommentAiRun run) throws WdkModelException {
    try (Connection con = _commentDs.getConnection()) {
      new InsertCommentAiRunQuery(_config.getCommentSchema(), run).run(con);
    } catch (SQLException ex) {
      throw new WdkModelException(ex);
    }
  }

  /**
   * Hydrate the optional {@code aiProvenance} field on each comment that has a
   * published AI provenance row, in a single batched join over
   * {@code comment_ai_provenance} + {@code comment_ai_run}. A no-op for an empty
   * batch; comments without provenance are left with a null (omitted) field.
   */
  private void attachAiProvenance(Connection con, Collection<Comment> comments)
      throws SQLException {
    if (comments.isEmpty())
      return;

    final List<Long> ids = new ArrayList<>(comments.size());
    for (Comment comment : comments)
      ids.add(comment.getId());

    final Map<Long, AiProvenanceView> byId =
        new GetCommentAiProvenanceQuery(_config.getCommentSchema(), ids).run(con).value();
    if (byId.isEmpty())
      return;

    for (Comment comment : comments)
      comment.setAiProvenance(byId.get(comment.getId()));
  }

  public boolean commentExists(long commentId) throws WdkModelException {
    try(Connection con = _commentDs.getConnection()) {
      return new GetCommentExistsQuery(_config.getCommentSchema(), commentId)
          .run(con).value();
    } catch (SQLException ex) {
      throw new WdkModelException(ex);
    }
  }

  /**
   * Retrieve comments by author and/or target
   *
   * @param author   comment author
   * @param stableId comment target id
   * @param type     comment target type
   *
   * @return Collection of comments matching the given search criteria
   */
  public Collection<Comment> queryComments(Long author, String stableId,
      String type) throws WdkModelException {

    final FindCommentQuery query = new FindCommentQuery(_config.getCommentSchema());
    final Collection<Comment> out;

    if(stableId != null && type != null) {
      if(author != null)
        query.setFilter(author, type, stableId);
      else
        query.setFilter(type, stableId);
    } else {
      query.setFilter(author);
    }

    try(Connection con = _commentDs.getConnection()) {
      out = query.run(con).value();
      for (Comment comment : out) {
        comment.setPubMedRefs(getPubMedRefs(comment));
      }
      attachAiProvenance(con, out);
    } catch (SQLException | IOException e) {
      throw new WdkModelException(e);
    }

    return out;
  }

  public void createAttachment(long commentId, Attachment att) throws WdkModelException {
    try(Connection con = _commentDs.getConnection()) {
      new InsertAttachmentQuery(_config.getCommentSchema(), commentId, att)
          .run(con);
    } catch (SQLException e) {
      throw new WdkModelException(e);
    }
  }

  public void updateAuthor(Author author) throws WdkModelException {
    try(Connection con = _commentDs.getConnection()) {
      new UpdateAuthorQuery(_config.getCommentSchema(), author).run(con);
    } catch (SQLException e) {
      throw new WdkModelException(e);
    }
  }

  public long createComment(CommentRequest com, User user)
      throws WdkModelException {
    final String schema = _config.getCommentSchema();
    long commentId;

    try (Connection con = _commentDs.getConnection()) {
      con.setAutoCommit(false);
      commentId = getNextId(Table.COMMENTS);

      try {
        if (!new GetAuthorQuery(schema, user.getUserId()).run(con).value().isPresent())
          new InsertAuthorQuery(schema, Author.fromUser(user)).run(con);

        new InsertCommentQuery(schema, commentId, com, user, _project).run(con);

        new InsertStableIdQuery(schema, commentId, filterStableIds(com),
            this::getNextId).run(con);

        final InsertReferencesQuery refQuery = new InsertReferencesQuery(schema,
            commentId, this::getNextId);

        if (!com.getDigitalObjectIds().isEmpty())
          refQuery.load(DIGITAL_OBJECT_ID, com.getDigitalObjectIds()).run(con);
        if (!com.getGenBankAccessions().isEmpty())
          refQuery.load(ACCESSION, com.getGenBankAccessions()).run(con);
        if (!com.getAdditionalAuthors().isEmpty())
          refQuery.load(AUTHOR, com.getAdditionalAuthors()).run(con);
        if (!com.getPubMedIds().isEmpty())
          refQuery.load(PUB_MED, com.getPubMedIds()).run(con);
        if (!com.getCategoryIds().isEmpty())
          new InsertCategoryQuery(schema, commentId, com.getCategoryIds(), this::getNextId).run(con);
        if (com.getSequence() != null && !com.getSequence().isEmpty())
          new InsertSequenceQuery(schema, com.getSequence(), commentId, this::getNextId).run(con);
        if (com.getLocation() != null)
          new InsertLocationQuery(schema, commentId, com.getLocation(), this::getNextId).run(con);
        if (com.getPreviousCommentId() != null)
          new UpdateAttachmentQuery(schema, com.getPreviousCommentId(), commentId).run(con);
        if (com.getExternalDatabase() != null)
          saveExternalDb(con, commentId, com.getExternalDatabase());
        if (com.getAiProvenance() != null) {
          com.getAiProvenance().setCommentId(commentId);
          new InsertCommentAiProvenanceQuery(schema, com.getAiProvenance()).run(con);
        }
      } catch (Throwable e) {
        con.rollback();
        throw e;
      }

      con.commit();
      new UserCommentUpdater(_config.getSolrUrl(), _commentDb, _config.getCommentSchema()).updateSingle(commentId);
    }
    catch (SQLException e) {
      throw new WdkModelException(e);
    }

    return commentId;
  }

  public void deleteComment(long commentId) throws WdkModelException {
    try(Connection con = _commentDs.getConnection()) {
      new HideCommentQuery(_config.getCommentSchema(), commentId).run(con);
      new UserCommentUpdater(_config.getSolrUrl(), _commentDb, _config.getCommentSchema())
        .updateSingle(commentId);
    } catch (SQLException e) {
      throw new WdkModelException(e);
    }
  }

  public void deleteAttachment(long commentId, long attachmentId)
    throws WdkModelException {
    try(Connection con = _commentDs.getConnection()) {
      new DeleteAttachmentQuery(_config.getCommentSchema(), commentId,
          attachmentId).run(con);
    } catch (SQLException e) {
      throw new WdkModelException(e);
    }
  }

  /**
   * Get the attachment with the given comment and file ids if exists.
   *
   * @param commentId    ID of the comment the attachment should be linked to
   * @param attachmentId ID of the attachment file to get
   */
  public Optional<Attachment> getAttachment(long commentId, long attachmentId)
      throws WdkModelException {
    try(Connection con = _commentDs.getConnection()) {
      return new GetAttachmentQuery(_config.getCommentSchema(), commentId,
          attachmentId).run(con).value();
    } catch (SQLException e) {
      throw new WdkModelException(e);
    }
  }

  public void close() throws Exception {
    if (!_isReusingUserDb) {
      _commentDb.close();
    }
  }

  public Collection<String> getInvalidStableIds(Collection<String> stableIds)
  throws WdkModelException {
    final String sql = "SELECT source_id FROM apidbtuning.GeneAttributes\n" +
      "WHERE source_id = ?\n" +
      "UNION\n" +
      "SELECT name FROM apidbtuning.samples\n" +
      "WHERE name = ?\n" +
      "UNION\n" +
      "SELECT source_id FROM DoTS.ExternalNASequence\n" +
      "WHERE source_id = ?\n" +
      "UNION\n" +
      "SELECT id FROM apidbtuning.GeneId\n" +
      "WHERE id = ? ";

    final Collection<String> errs = new ArrayList<>();
    try(
      Connection con = _wdkModel.getAppDb().getDataSource().getConnection();
      PreparedStatement ps = con.prepareStatement(sql)
    ) {
      for (String sourceId : stableIds) {
        ps.setString(1, sourceId);
        ps.setString(2, sourceId);
        ps.setString(3, sourceId);
        ps.setString(4, sourceId);
        try (ResultSet rs = ps.executeQuery()) {
          if (!rs.next())
            errs.add(sourceId);
        }
      }
    } catch (SQLException e) {
      throw new WdkModelException(e);
    }

    return errs;
  }

  private void initialize(WdkModel model, DatabaseInstance commentDb,
    CommentConfig config, boolean isReusingUserDb, Project project, String host
  ) {
    _wdkModel = model;
    _project = project;
    _host = host;
    _commentDb = commentDb;
    _commentDs = commentDb.getDataSource();
    _config = config;
    _isReusingUserDb = isReusingUserDb;
  }

  /**
   * Increment and return the new current value in the ID sequence for the given
   * table
   *
   * @param tableName table to get the next id for
   * @return Incremented ID for the given table
   */
  private long getNextId(String tableName) throws SQLException {
    return _commentDb.getPlatform().getNextId(_commentDs,
        _config.getCommentSchema(), tableName);
  }

  private void saveExternalDb(Connection con, long commentId,
    ExternalDatabase extDB) throws SQLException {
    final String schema = _config.getCommentSchema();

    Optional<Long> externalDbId = new GetExternalDatabaseQuery(schema,
        extDB.getName(), extDB.getVersion()).run(con).value();

    long id = externalDbId.isPresent()
        ? externalDbId.get()
        : new InsertExternalDatabaseQuery(schema, extDB, this::getNextId)
          .run(con).value();

    new InsertExternalDatabaseLinkQuery(schema, commentId, id).run(con);
  }

  /**
   * Retrieve PubMed reference details.
   * <p>
   * Pulls the IDs from the database, then pulls the details from an HTTP
   * request to the perl script cgi-bin/pmid2json.
   *
   * @param com ID of the comment for which to lookup PubMed links
   */
  private List<PubMedReference> getPubMedRefs(Comment com) throws IOException {
    final var numericIds = new ArrayList<String>(com.getPubMedIds().size());
    final var invalidIds = new ArrayList<String>(com.getPubMedIds().size()/2);

    com.getPubMedIds().forEach(id -> {
      for (var i = 0; i < id.length(); i++) {
        if (id.charAt(i) < '0' || id.charAt(i) > '9') {
          invalidIds.add(id);
          return;
        }
      }
      numericIds.add(id);
    });

    final var url = Utilities.newURL(_host + "/cgi-bin/pmid2json?pmids=" + String.join(",", numericIds));

    try (InputStream in = url.openStream()) {
      return Stream.concat(
        JSON.readerForListOf(PubMedReference.class)
          .<Collection<PubMedReference>>readValue(in)
          .stream()
          .filter(ref -> {
            if (Objects.equals(ref.getStatus(), "FOUND")) {
              return true;
            } else {
              invalidIds.add(ref.getId());
              return false;
            }
          }),
        invalidIds.stream().map(id -> new PubMedReference(id, null, null, null, null, null))
      ).collect(Collectors.toList());
    }
  }

  /**
   * Create a new set containing the related stable ids excluding the target
   * stable id for the comment.
   *
   * @param com Comment for which to get a filtered list of related stable ids
   *
   * @return A filtered list of stable ids, excluding the comment target id
   */
  private Set<String> filterStableIds(CommentRequest com) {
    final Set<String> set = new HashSet<>(com.getRelatedStableIds());
    set.remove(com.getTarget().getId());
    return set;
  }

  public Collection<Attachment> getAllAttachments(long commentId)
      throws WdkModelException {
    try(Connection con = _commentDs.getConnection()) {
      return new GetAllAttachmentsQuery(_config.getCommentSchema(), commentId)
          .run(con).value();
    } catch (SQLException e) {
      throw new WdkModelException(e);
    }
  }
}
