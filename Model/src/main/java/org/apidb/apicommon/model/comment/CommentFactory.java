package org.apidb.apicommon.model.comment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apidb.apicommon.model.comment.pojo.*;
import org.apidb.apicommon.model.comment.repo.*;
import org.gusdb.fgputil.db.ConnectionMapping;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.runtime.InstanceManager;
import org.gusdb.fgputil.runtime.Manageable;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.config.ModelConfigUserDB;
import org.gusdb.wdk.model.user.User;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Optional;

import static org.apidb.apicommon.model.comment.ReferenceType.*;

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
        // if connections are the same, then ignore comment config connection info and use UserDB database instance
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

      factory.initialize(db, config, isReusingUserDb,
          new Project(projectId, wdkModel.getVersion()), host.endsWith("/")
              ? host.substring(0, host.lastIndexOf("/")) : host);

      return factory;
    }
    catch (CommentModelException ex) {
      throw new WdkModelException();
    }
  }

  public CommentConfig getCommentConfig() {
    return _config;
  }

  public DataSource getCommentDataSource() {
    return _commentDs;
  }

  public Optional<Comment> getComment(long commentId) throws WdkModelException {
    try(Connection con  = ConnectionMapping.getConnection(_commentDs)) {
      return new GetCommentQuery(_config.getCommentSchema(), commentId)
          .run(con).value();
    } catch (SQLException ex) {
      throw new WdkModelException(ex);
    }
  }

  public boolean commentExists(long commentId) throws WdkModelException {
    try(Connection con = ConnectionMapping.getConnection(_commentDs)) {
      return new GetCommentExistsQuery(_config.getCommentSchema(), commentId)
          .run(con).value();
    } catch (SQLException ex) {
      throw new WdkModelException(ex);
    }
  }

  /**
   * Retrieve comments by author
   *
   * @param author comment author
   *
   * @return Collection of comments matching the given author
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

    try(Connection con = ConnectionMapping.getConnection(_commentDs)) {
      out = query.run(con).value();
      for (Comment comment : out) {
        getPubMedRefs(comment);
      }
    } catch (SQLException | IOException e) {
      throw new WdkModelException(e);
    }

    return out;
  }

  public void createAttachment(long commentId, Attachment att) throws WdkModelException {
    try(Connection con = ConnectionMapping.getConnection(_commentDs)) {
      new InsertAttachmentQuery(_config.getCommentSchema(), commentId, att)
          .run(con);
    } catch (SQLException e) {
      throw new WdkModelException(e);
    }
  }

  public void updateAuthor(Author author) throws WdkModelException {
    try(Connection con = ConnectionMapping.getConnection(_commentDs)) {
      new UpdateAuthorQuery(_config.getCommentSchema(), author).run(con);
    } catch (SQLException e) {
      throw new WdkModelException(e);
    }
  }
//
//  public ArrayList<MultiBox> getMultiBoxData(String nameCol, String valueCol,
//      String table, String condition) throws WdkModelException {
//
//    ArrayList<MultiBox> list = new ArrayList<>();
//
//    StringBuilder sql = new StringBuilder();
//    sql.append("SELECT ").append(nameCol).append(",").append(valueCol);
//    sql.append(" FROM  ").append(_config.getCommentSchema()).append(table);
//    if (condition != null) {
//      sql.append(" WHERE ").append(condition);
//    }
//
//    MultiBox multiBox;
//
//    try (Connection con = ConnectionMapping.getConnection(
//        _commentDs); PreparedStatement ps = con.prepareStatement(
//        sql.toString())) {
//      try (ResultSet rs = ps.executeQuery()) {
//
//        while (rs.next()) {
//          String name = rs.getString(nameCol);
//          int value = rs.getInt(valueCol);
//          multiBox = new MultiBox(name, value + "");
//          list.add(multiBox);
//        }
//        return list;
//      }
//    }
//    catch (SQLException e) {
//      throw new WdkModelException(e);
//    }
//  }

  public long createComment(CommentRequest comment, User user)
      throws WdkModelException {
    final String schema = _config.getCommentSchema();
    long commentId;

    try (Connection con = ConnectionMapping.getConnection(_commentDs)) {
      con.setAutoCommit(false);
      commentId = getNextId(Table.COMMENTS);

      if (!new GetAuthorQuery(schema, user.getUserId()).run(con).value().isPresent())
        new InsertAuthorQuery(schema, Author.fromUser(user)).run(con);

      new InsertCommentQuery(schema, commentId, comment, user, _project).run(con);

      final InsertReferencesQuery refQuery = new InsertReferencesQuery(schema,
          commentId, this::getNextId);
      refQuery.load(DIGITAL_OBJECT_ID, comment.getDigitalObjectIds()).run(con);
      refQuery.load(ACCESSION, comment.getGenBankAccessions()).run(con);
      refQuery.load(AUTHOR, comment.getAdditionalAuthors()).run(con);
      refQuery.load(PUB_MED, comment.getPubMedIds()).run(con);

      new InsertLocationQuery(schema, commentId, comment.getLocations(),
          this::getNextId).run(con);
      new InsertCategoryQuery(schema, commentId, comment.getCategoryIds(),
          this::getNextId).run(con);
      new InsertStableIdQuery(schema, commentId, comment.getRelatedStableIds(),
          this::getNextId).run(con);
      new InsertSequenceQuery(schema, comment.getSequence(), commentId,
          this::getNextId).run(con);

      saveExternalDb(con, commentId, comment.getExternalDb());

      con.commit();
    }
    catch (SQLException e) {
      throw new WdkModelException(e);
    }

    return commentId;
  }

  public void deleteComment(long commentId) throws WdkModelException {
    try(Connection con = ConnectionMapping.getConnection(_commentDs)) {
      new HideCommentQuery(_config.getCommentSchema(), commentId).run(con);
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
    try(Connection con = ConnectionMapping.getConnection(_commentDs)) {
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

  private void initialize(DatabaseInstance commentDb, CommentConfig config,
      boolean isReusingUserDb, Project project, String host) {
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
   *
   * Pulls the IDs from the database, then pulls the details from an HTTP
   * request to the perl script cgi-bin/pmid2json.
   *
   * @param com ID of the comment for which to lookup PubMed links
   */
  private void getPubMedRefs(Comment com)
      throws IOException {
    final URL url = new URL(_host + "/cgi-bin/pmid2json?pmids=" + String.join(
        ",", com.getPubMedIds()));
    com.setPubMedRefs(
        JSON.readerFor(new TypeReference<Collection<PubMedReference>>() {})
            .readValue(url));
  }
}
