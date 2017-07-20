package org.apidb.apicommon.model.comment;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.accountdb.UserProfile;
import org.gusdb.fgputil.db.SqlUtils;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.db.runner.SingleLongResultSetHandler;
import org.gusdb.fgputil.runtime.InstanceManager;
import org.gusdb.fgputil.runtime.Manageable;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.config.ModelConfigUserDB;
import org.gusdb.wdk.model.user.UserFactory;

/**
 * Manages user comments on WDK records
 * 
 * @author xingao
 */
public class CommentFactory implements Manageable<CommentFactory> {

  private static final Logger LOG = Logger.getLogger(CommentFactory.class);

  private DatabaseInstance _commentDb;
  private DataSource _commentDs;
  private CommentConfig _config;
  private boolean _isReusingUserDb;
  private UserFactory _userFactory;

  @Override
  public CommentFactory getInstance(String projectId, String gusHome) throws Exception {
    // parse and load the configuration
    CommentConfigParser parser = new CommentConfigParser(gusHome);
    boolean isReusingUserDb;
    try {
      CommentConfig config = parser.parseConfig(projectId);

      // create a platform object
      ModelConfigUserDB userDbConfig = WdkModel.getModelConfig(projectId, gusHome).getUserDB();
      DatabaseInstance db;
      if (userDbConfig.isSameConnectionInfoAs(config)) {
        // if connections are the same, then ignore comment config connection info and use UserDB database instance
        LOG.info("Will reuse USER_DB connection pool since connection information is the same.");
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
      WdkModel wdkModel = InstanceManager.getInstance(WdkModel.class, gusHome, projectId);
      factory.initialize(db, config, isReusingUserDb, wdkModel.getUserFactory());
      return factory;
    }
    catch (CommentModelException ex) {
      throw new WdkModelException();
    }
  }

  private void initialize(DatabaseInstance commentDb, CommentConfig config, boolean isReusingUserDb, UserFactory userFactory) {
    this._commentDb = commentDb;
    this._commentDs = commentDb.getDataSource();
    this._config = config;
    this._isReusingUserDb = isReusingUserDb;
    this._userFactory = userFactory;
  }

  public CommentTarget getCommentTarget(String internalValue) throws WdkModelException {

    // load the comment target definition of the given internal value

    ResultSet rs = null;
    CommentTarget target = null;
    String query = null;
    PreparedStatement ps = null;
    try {
      query = "SELECT * FROM " + _config.getCommentSchema() + "comment_target WHERE comment_target_id=?";
      ps = SqlUtils.getPreparedStatement(_commentDs, query);
      ps.setString(1, internalValue);
      rs = ps.executeQuery();
      if (!rs.next())
        throw new WdkModelException("The comment target cannot be " +
            "found with the given internal value: " + internalValue);

      target = new CommentTarget(internalValue);
      target.setDisplayName(rs.getString("comment_target_name"));
      target.setRequireLocation((rs.getInt("require_location") != 0));
    }
    catch (SQLException ex) {
      throw new WdkModelException(ex);
    }
    finally {
      // close the connection
      SqlUtils.closeResultSetAndStatement(rs, ps);
    }
    return target;
  }

  private long getNextId(String tableName) throws SQLException {
    return _commentDb.getPlatform().getNextId(_commentDs, _config.getCommentSchema(), tableName);
  }

  public void addComment(Comment comment) throws WdkModelException {
    addComment(comment, null);
  }

  public void addComment(Comment comment, String previousCommentId) throws WdkModelException {
    String commentSchema = _config.getCommentSchema();
    PreparedStatement ps = null;
    // get a new comment id
    try {

      long userId = comment.getUserId();
      long commentId = getNextId("comments");
      long[] targetCategoryIds = comment.getTargetCategoryIds();
      String[] pmIds = comment.getPmIds();
      String[] dois = comment.getDois();
      String[] accessions = comment.getAccessions();
      String[] files = comment.getFiles();
      String[] existingFiles = comment.getExistingFiles();
      String[] associatedStableIds = comment.getAssociatedStableIds();
      String[] authors = comment.getAuthors();
      String sequence = comment.getSequence();

      // first need to add user to comments DB if not already present
      if (!userPresentInCommentUsersTable(userId)) {
        insertCommentUser(userId);
      }

      ps = SqlUtils.getPreparedStatement(_commentDs, "INSERT INTO " + commentSchema +
          "comments (comment_id, " + "comment_date, comment_target_id, " +
          "stable_id, conceptual, project_name, " + "project_version, headline, content, " +
          "location_string, review_status_id, organism, user_id) " + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)");

      long currentMillis = System.currentTimeMillis();

      ps.setLong(1, commentId);
      ps.setTimestamp(2, new Timestamp(currentMillis));
      ps.setString(3, comment.getCommentTarget());
      ps.setString(4, comment.getStableId());
      ps.setBoolean(5, comment.isConceptual());
      ps.setString(6, comment.getProjectName());
      ps.setString(7, comment.getProjectVersion());
      ps.setString(8, comment.getHeadline());
      _commentDb.getPlatform().setClobData(ps, 9, comment.getContent(), false);
      ps.setString(10, comment.getLocationString());
      String reviewStatus = (comment.getReviewStatus() != null && comment.getReviewStatus().length() > 0)
          ? comment.getReviewStatus() : Comment.COMMENT_REVIEW_STATUS_UNKNOWN;
      ps.setString(11, reviewStatus);
      ps.setString(12, comment.getOrganism());
      ps.setLong(13, userId);

      int result = ps.executeUpdate();
      LOG.debug("Inserted comment row: " + result);

      // update the fields of comment
      comment.setCommentDate(new java.util.Date(currentMillis));
      comment.setCommentId(commentId);

      // then add the location information
      saveLocations(commentId, comment);

      // then add the eternal database information
      saveExternalDbs(commentId, comment);

      if ((targetCategoryIds != null) && (targetCategoryIds.length > 0)) {
        saveCommentTargetCategory(commentId, targetCategoryIds);
      }

      if ((pmIds != null) && (pmIds.length > 0)) {
        savePmIds(commentId, pmIds);
      }

      if ((dois != null) && (dois.length > 0)) {
        saveDois(commentId, dois);
      }

      if ((accessions != null) && (accessions.length > 0)) {
        saveAccessions(commentId, accessions);
      }

      LOG.debug(">>>>> sequene: is " + sequence);

      if ((sequence != null) && (sequence.trim().equals(""))) {
        saveSequence(commentId, sequence);
      }

      if ((files != null) && (files.length > 0)) {
        saveFiles(commentId, files);
      }

      if ((existingFiles != null) && (existingFiles.length > 0)) {
        updateFiles(commentId, existingFiles);
      }

      if ((associatedStableIds != null) && (associatedStableIds.length > 0)) {
        saveAssociatedStableIds(commentId, associatedStableIds);
      }

      if ((authors != null) && (authors.length > 0)) {
        saveAuthors(commentId, authors);
      }

      if (comment.getCommentTarget().equalsIgnoreCase("phenotype")) {
        savePhenotype(commentId, comment.getBackground(), comment.getMutantStatus(),
            comment.getMutationType(), comment.getMutationMethod(), comment.getMutantExpression(),
            comment.getPhenotypeLoc(), comment.getContent());
        saveMutantMarkers(commentId, comment.getMutantMarkers());
        saveMutantReporters(commentId, comment.getMutantReporters());
        savePhenotypeCategory(commentId, comment.getPhenotypeCategory());
      }

      // get a new comment in order to fetch the user info
      Comment newComment = getComment(commentId);

      comment.setUserName(newComment.getUserName());
      comment.setOrganization(newComment.getOrganization());

      if ((previousCommentId != null) && (previousCommentId.length() != 0)) {
        setInvisibleComment(previousCommentId);
        updatePrevCommentId(previousCommentId, commentId);
      }

    }
    catch (SQLException ex) {
      ex.printStackTrace();
      throw new WdkModelException(ex);
    }
    finally {
      SqlUtils.closeStatement(ps);
    }

    // print connection status
    printStatus();
  }

  private boolean userPresentInCommentUsersTable(long userId) {
    String sql = "select count(*) from " + _config.getCommentSchema() + "comment_users where user_id = ?";
    SingleLongResultSetHandler result = new SQLRunner(_commentDs, sql).executeQuery(
        new Object[]{ userId }, new Integer[]{ Types.BIGINT }, new SingleLongResultSetHandler());
    return result.getRetrievedValue() > 0;
  }

  private void insertCommentUser(long userId) throws WdkModelException {
    Map<String,String> props = _userFactory.getUserById(userId).getProfileProperties();
    String sql = "insert into " + _config.getCommentSchema() + "comment_users " +
        "(user_id, first_name, last_name, organization) values (?, ?, ?, ?)";
    new SQLRunner(_commentDs, sql).executeStatement(
        new Object[] { userId, props.get("firstName"), props.get("lastName"), props.get("organization") },
        new Integer[] { Types.BIGINT, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR });
  }

  public void updateCommentUser(UserProfile userProfile) {
    Map<String,String> props = userProfile.getProperties();
    String sql = "update " + _config.getCommentSchema() + "comment_users set " +
        "first_name = ?, last_name = ?, organization = ? where user_id = ?";
    new SQLRunner(_commentDs, sql).executeUpdate(
        new Object[] { props.get("firstName"), props.get("lastName"), props.get("organization"), userProfile.getUserId() },
        new Integer[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.BIGINT });
    
  }

  private void saveLocations(long commentId, Comment comment) throws SQLException {
    String commentSchema = _config.getCommentSchema();
    // construct sql
    StringBuffer sql = new StringBuffer();
    sql.append("INSERT INTO " + commentSchema + "locations (comment_id, ");
    sql.append("location_id, location_start, location_end, is_reverse, ");
    sql.append("coordinate_type) VALUES (?, ?, ?, ?, ?, ?)");
    PreparedStatement statement = null;
    try {
      statement = SqlUtils.getPreparedStatement(_commentDs, sql.toString());

      Location[] locations = comment.getLocations();
      for (Location location : locations) {
        long locationId = getNextId("locations");
        statement.setLong(1, commentId);
        statement.setLong(2, locationId);
        statement.setLong(3, location.getLocationStart());
        statement.setLong(4, location.getLocationEnd());
        statement.setBoolean(5, location.isReversed());
        statement.setString(6, location.getCoordinateType());
        statement.execute();
      }
    }
    finally {
      SqlUtils.closeStatement(statement);
    }
  }

  private void savePhenotype(long commentId, String background, int mutantStatus, int mutationType,
      int mutationMethod, int mutantExpression, int phenotypeLoc, String phenotypeDescription)
      throws SQLException {

    String commentSchema = _config.getCommentSchema();

    // construct sql
    StringBuffer sql = new StringBuffer();
    sql.append("INSERT INTO " + commentSchema + "Phenotype ");
    sql.append("(phenotype_id, comment_id, ");
    sql.append("background, mutant_status_id, mutant_type_id, ");
    sql.append("mutant_method_id, mutant_expression_id, ");
    sql.append("phenotype_loc_id, phenotype_description ");
    sql.append(") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
    PreparedStatement statement = null;
    try {
      statement = SqlUtils.getPreparedStatement(_commentDs, sql.toString());

      statement.setLong(1, getNextId("phenotype"));
      statement.setLong(2, commentId);
      statement.setString(3, background);
      statement.setInt(4, mutantStatus);
      statement.setInt(5, mutationType);
      statement.setInt(6, mutationMethod);
      statement.setInt(7, mutantExpression);
      statement.setInt(8, phenotypeLoc);
      statement.setString(9, phenotypeDescription);
      statement.execute();
    }
    finally {
      SqlUtils.closeStatement(statement);
    }
  }

  private void saveMutantMarkers(long commentId, int[] mutantMarkers) throws SQLException {
    String commentSchema = _config.getCommentSchema();

    StringBuffer sql = new StringBuffer();
    sql.append("INSERT INTO " + commentSchema + "PhenotypeMutantMarker ");
    sql.append("(comment_mutant_marker_id, comment_id, ");
    sql.append("mutant_marker_id ");
    sql.append(") VALUES (?, ?, ?)");
    PreparedStatement statement = null;
    try {
      statement = SqlUtils.getPreparedStatement(_commentDs, sql.toString());

      for (int mutantMarker : mutantMarkers) {
        statement.setLong(1, getNextId("commentMutantMarker"));
        statement.setLong(2, commentId);
        statement.setInt(3, mutantMarker);
        statement.execute();
      }
    }
    finally {
      SqlUtils.closeStatement(statement);
    }
  }

  private void saveMutantReporters(long commentId, int[] mutantReporters) throws SQLException {
    String commentSchema = _config.getCommentSchema();

    StringBuffer sql = new StringBuffer();
    sql.append("INSERT INTO " + commentSchema + "PhenotypeMutantReporter ");
    sql.append("(comment_mutant_reporter_id, comment_id, ");
    sql.append("mutant_reporter_id ");
    sql.append(") VALUES (?, ?, ?)");
    PreparedStatement statement = null;
    try {
      statement = SqlUtils.getPreparedStatement(_commentDs, sql.toString());

      for (int mutantReporter : mutantReporters) {
        statement.setLong(1, getNextId("commentMutantReporter"));
        statement.setLong(2, commentId);
        statement.setInt(3, mutantReporter);
        statement.execute();
      }
    }
    finally {
      SqlUtils.closeStatement(statement);
    }
  }

  private void savePhenotypeCategory(long commentId, int[] phenotypeCategory) throws SQLException {
    String commentSchema = _config.getCommentSchema();

    StringBuffer sql = new StringBuffer();
    sql.append("INSERT INTO " + commentSchema + "PhenotypeMutantCategory ");
    sql.append("(comment_mutant_category_id, comment_id, ");
    sql.append("mutant_category_id ");
    sql.append(") VALUES (?, ?, ?)");
    PreparedStatement statement = null;
    try {
      statement = SqlUtils.getPreparedStatement(_commentDs, sql.toString());

      for (int cat : phenotypeCategory) {
        statement.setLong(1, getNextId("phenotypeMutantCategory"));
        statement.setLong(2, commentId);
        statement.setInt(3, cat);
        statement.execute();
      }
    }
    finally {
      SqlUtils.closeStatement(statement);
    }
  }

  private void saveCommentTargetCategory(long commentId, long[] targetCategoryIds) throws SQLException {
    String commentSchema = _config.getCommentSchema();

    // construct sql
    StringBuffer sql = new StringBuffer();
    sql.append("INSERT INTO " + commentSchema + "CommentTargetCategory ");
    sql.append("(comment_target_category_id, comment_id, ");
    sql.append("target_category_id ");
    sql.append(") VALUES (?, ?, ?)");
    PreparedStatement statement = null;
    try {
      statement = SqlUtils.getPreparedStatement(_commentDs, sql.toString());

      for (long targetCategoryId : targetCategoryIds) {
        statement.setLong(1, getNextId("commentTargetCategory"));
        statement.setLong(2, commentId);
        statement.setLong(3, targetCategoryId);
        statement.execute();
      }
    }
    finally {
      SqlUtils.closeStatement(statement);
    }
  }

  private void savePmIds(long commentId, String[] pmIds) throws SQLException {
    String commentSchema = _config.getCommentSchema();

    // construct sql
    StringBuffer sql = new StringBuffer();
    sql.append("INSERT INTO " + commentSchema + "CommentReference ");
    sql.append("(comment_reference_id, source_id, ");
    sql.append("database_name, comment_id ");
    sql.append(") VALUES (?, ?, ?, ?)");
    PreparedStatement statement = null;
    try {
      statement = SqlUtils.getPreparedStatement(_commentDs, sql.toString());

      for (String pmId : pmIds) {
        if ((pmId != null) && (pmId.trim().length() != 0)) {
          statement.setLong(1, getNextId("commentReference"));
          statement.setString(2, pmId);
          statement.setString(3, "pubmed");
          statement.setLong(4, commentId);
          statement.execute();
        }
      }
    }
    finally {
      SqlUtils.closeStatement(statement);
    }
  }

  private void saveDois(long commentId, String[] dois) throws SQLException {
    String commentSchema = _config.getCommentSchema();

    // construct sql
    StringBuffer sql = new StringBuffer();
    sql.append("INSERT INTO " + commentSchema + "CommentReference ");
    sql.append("(comment_reference_id, source_id, ");
    sql.append("database_name, comment_id ");
    sql.append(") VALUES (?, ?, ?, ?)");
    PreparedStatement statement = null;
    try {
      statement = SqlUtils.getPreparedStatement(_commentDs, sql.toString());

      for (String doi : dois) {
        if ((doi != null) && (doi.trim().length() != 0)) {
          statement.setLong(1, getNextId("commentReference"));
          statement.setString(2, doi);
          statement.setString(3, "doi");
          statement.setLong(4, commentId);
          statement.execute();
        }
      }
    }
    finally {
      SqlUtils.closeStatement(statement);
    }
  }

  private void saveSequence(long commentId, String sequence) throws SQLException {
    String commentSchema = _config.getCommentSchema();

    // construct sql
    StringBuffer sql = new StringBuffer();
    sql.append("INSERT INTO " + commentSchema + "CommentSequence ");
    sql.append("(comment_sequence_id, sequence, ");
    sql.append(" comment_id ");
    sql.append(") VALUES (?, ?, ?)");

    LOG.debug(">>>>>>>> " + sql.toString());
    PreparedStatement statement = null;
    try {
      statement = SqlUtils.getPreparedStatement(_commentDs, sql.toString());
      statement.setLong(1, getNextId("commentSequence"));
      statement.setString(2, sequence);
      statement.setLong(3, commentId);
      statement.execute();
    }
    finally {
      SqlUtils.closeStatement(statement);
    }
  }

  private void saveAccessions(long commentId, String[] accessions) throws SQLException {
    String commentSchema = _config.getCommentSchema();

    // construct sql
    StringBuffer sql = new StringBuffer();
    sql.append("INSERT INTO " + commentSchema + "CommentReference ");
    sql.append("(comment_reference_id, source_id, ");
    sql.append("database_name, comment_id ");
    sql.append(") VALUES (?, ?, ?, ?)");
    PreparedStatement statement = null;
    try {
      statement = SqlUtils.getPreparedStatement(_commentDs, sql.toString());

      for (String accession : accessions) {
        if ((accession != null) && (accession.trim().length() != 0)) {
          statement.setLong(1, getNextId("commentReference"));
          statement.setString(2, accession);
          statement.setString(3, "genbank");
          statement.setLong(4, commentId);
          statement.execute();
        }
      }
    }
    finally {
      SqlUtils.closeStatement(statement);
    }
  }

  private void saveFiles(long commentId, String[] files) throws SQLException {
    String commentSchema = _config.getCommentSchema();

    // construct sql
    StringBuffer sql = new StringBuffer();
    sql.append("INSERT INTO " + commentSchema + "CommentFile ");
    sql.append("(file_id, name, notes, ");
    sql.append(" comment_id ");
    sql.append(") VALUES (?, ?, ?, ?)");
    PreparedStatement statement = null;
    try {
      statement = SqlUtils.getPreparedStatement(_commentDs, sql.toString());

      for (String file : files) {
        if (file == null)
          continue;
        String[] str = file.split("\\|");
        statement.setLong(1, Long.parseLong(str[0]));
        statement.setString(2, str[1]);
        statement.setString(3, str[2]);
        statement.setLong(4, commentId);
        statement.execute();
      }
    }
    finally {
      SqlUtils.closeStatement(statement);
    }
  }

  private void updateFiles(long newCommentId, String[] files) throws SQLException {
    String commentSchema = _config.getCommentSchema();

    for (String file : files) {
      if (file == null)
        continue;
      String[] str = file.split("\\|");
      String sql = "UPDATE " + commentSchema + "CommentFile " + " SET comment_id = " + newCommentId +
          " WHERE file_id = " + Long.parseLong(str[0]);

      SqlUtils.executeUpdate(_commentDs, sql, "wdk-comment-update-comment-id");
    }
  }

  private void setInvisibleComment(String previousCommentId) throws SQLException {
    String commentSchema = _config.getCommentSchema();

    String sql = "UPDATE " + commentSchema + "comments " + " SET is_visible = 0" + " WHERE comment_id = '" +
        previousCommentId + "'";

    SqlUtils.executeUpdate(_commentDs, sql, "wdk-comment-update-visible");

  }

  private void updatePrevCommentId(String previousCommentId, long commentId) throws SQLException {
    String commentSchema = _config.getCommentSchema();

      String sql = "UPDATE " + commentSchema + "comments " + " SET prev_comment_id = " + previousCommentId +
          " WHERE comment_id = " + commentId;

      SqlUtils.executeUpdate(_commentDs, sql, "wdk-comment-update-previous-comment-id");

  }

  private void saveAssociatedStableIds(long commentId, String[] associatedStableIds) throws SQLException {
    String commentSchema = _config.getCommentSchema();

    // removing duplicates 
    Set<String> stringSet = new HashSet<>(Arrays.asList(associatedStableIds));
    String[] associatedStableIds_noDup = stringSet.toArray(new String[0]);

    // construct sql
    StringBuffer sql = new StringBuffer();
    sql.append("INSERT INTO " + commentSchema + "CommentStableId ");
    sql.append("(comment_stable_id, stable_id, ");
    sql.append(" comment_id ");
    sql.append(") VALUES (?, ?, ?)");
    PreparedStatement statement = null;

    try {
      statement = SqlUtils.getPreparedStatement(_commentDs, sql.toString());

      for (String associatedStableId : associatedStableIds_noDup) {
        if ((associatedStableId != null) && (associatedStableId.trim().length() != 0)) {
          statement.setLong(1, getNextId("commentStableId"));
          statement.setString(2, associatedStableId);
          statement.setLong(3, commentId);
          statement.execute();
        }
      }
    }
    finally {
      SqlUtils.closeStatement(statement);
    }
  }

  private void saveAuthors(long commentId, String[] authors) throws SQLException {
    String commentSchema = _config.getCommentSchema();

    StringBuffer sql = new StringBuffer();
    sql.append("INSERT INTO " + commentSchema + "CommentReference ");
    sql.append("(comment_reference_id, source_id, ");
    sql.append("database_name, comment_id ");
    sql.append(") VALUES (?, ?, ?, ?)");
    PreparedStatement statement = null;
    try {
      statement = SqlUtils.getPreparedStatement(_commentDs, sql.toString());

      for (String author : authors) {
        if ((author != null) && (author.trim().length() != 0)) {
          statement.setLong(1, getNextId("commentReference"));
          statement.setString(2, author);
          statement.setString(3, "author");
          statement.setLong(4, commentId);
          statement.execute();
        }
      }
    }
    finally {
      SqlUtils.closeStatement(statement);
    }
  }

  /**
   * For the first release, this method hard-code the external database name and version, instead of looking
   * up directly in GUS. This avoids the backward dblink requirement; The site should provide such information
   * to the comment factory in future releases.
   * 
   * UPDATE: Assume that the Comment parameter already has the external database info filled in. Use the first
   * extdb/versions (Is there a case at all where we need more than one ext db???)
   * 
   * @param commentId
   * @param comment
   */
  private void saveExternalDbs(long commentId, Comment comment) throws SQLException {
    String commentSchema = _config.getCommentSchema();
    // String dblink = config.getProjectDbLink();
    // String stableId = comment.getStableId();

    // construct sqls
    StringBuffer sqlQueryDb = new StringBuffer();
    sqlQueryDb.append("SELECT external_database_id ");
    sqlQueryDb.append("FROM " + commentSchema + "external_databases ");
    sqlQueryDb.append("WHERE external_database_name = ? ");
    sqlQueryDb.append("AND external_database_version = ?");

    StringBuffer sqlInsertDb = new StringBuffer();
    sqlInsertDb.append("INSERT INTO " + commentSchema + "external_databases ");
    sqlInsertDb.append("(external_database_id, external_database_name, ");
    sqlInsertDb.append("external_database_version) VALUES (?, ?, ?)");

    StringBuffer sqlInsertLink = new StringBuffer();
    sqlInsertLink.append("INSERT INTO " + commentSchema + "comment_external_database ");
    sqlInsertLink.append("(external_database_id, comment_id) ");
    sqlInsertLink.append("VALUES (?, ?)");

    PreparedStatement psQueryDb = null, psInsertDb = null, psInsertLink = null;
    try {
      // construct prepared statements
      psQueryDb = SqlUtils.getPreparedStatement(_commentDs, sqlQueryDb.toString());

      psInsertDb = SqlUtils.getPreparedStatement(_commentDs, sqlInsertDb.toString());

      psInsertLink = SqlUtils.getPreparedStatement(_commentDs, sqlInsertLink.toString());

      // add every external database record into the database
      for (ExternalDatabase externalDb : comment.getExternalDbs()) {
        psQueryDb.setString(1, externalDb.getExternalDbName());
        psQueryDb.setString(2, externalDb.getExternalDbVersion());
        ResultSet rsQueryDb = null;
        long externalDbId;
        try {
          rsQueryDb = psQueryDb.executeQuery();
          if (!rsQueryDb.next()) {
            // external database entry doesn't exist
            externalDbId = getNextId("external_databases");
            psInsertDb.setLong(1, externalDbId);
            psInsertDb.setString(2, externalDb.getExternalDbName());
            psInsertDb.setString(3, externalDb.getExternalDbVersion());
            psInsertDb.execute();
          }
          else { // has entry, get the external_database_id
            externalDbId = rsQueryDb.getLong("external_database_id");
          }
        }
        finally {
          // the ResultSet is closed manually, instead of using
          // SqlUtilssince we need to keep the underline statement
          // open during the for loop
          if (rsQueryDb != null)
            rsQueryDb.close();
        }

        // add the reference link
        psInsertLink.setLong(1, externalDbId);
        psInsertLink.setLong(2, commentId);
        psInsertLink.execute();
      }
    }
    finally {
      // close statements and result set
      SqlUtils.closeStatement(psQueryDb);
      SqlUtils.closeStatement(psInsertDb);
      SqlUtils.closeStatement(psInsertLink);
    }
  }

  public Comment getComment(long commentId) throws WdkModelException {
    StringBuffer sql = new StringBuffer();
    sql.append("SELECT c.user_id, c.comment_date, c.comment_target_id, ");
    sql.append("c.conceptual, c.headline, c.project_name, ");
    sql.append("c.project_version, c.review_status_id, c.stable_id, ");
    sql.append("c.content, ");
    sql.append("substr(c.organism, 1, instr(c.organism || '  ', ' ', 1, 2)-1) as organism, ");
    sql.append("u.first_name || ' ' || u.last_name as user_name, ");
    sql.append("u.organization, c.content, c.review_status_id FROM ");
    sql.append(_config.getCommentSchema() + "comments c, ");
    sql.append(_config.getCommentSchema() + "comment_users u ");
    sql.append("WHERE c.user_id = u.user_id ");
    sql.append("AND c.comment_id = ? ");

    ResultSet rs = null;
    PreparedStatement ps = null;
    try {
      ps = SqlUtils.getPreparedStatement(_commentDs, sql.toString());
      ps.setLong(1, commentId);
      rs = ps.executeQuery();

      if (!rs.next())
        throw new WdkModelException("Comment of the given id '" + commentId + "' cannot be found.");

      // construct a comment object
      Comment comment = new Comment(rs.getLong("user_id"));
      comment.setCommentId(commentId);
      comment.setCommentDate(rs.getTimestamp("comment_date"));
      comment.setCommentTarget(rs.getString("comment_target_id"));
      comment.setConceptual(rs.getBoolean("conceptual"));
      comment.setHeadline(rs.getString("headline"));
      comment.setContent(rs.getString("content"));
      comment.setProjectName(rs.getString("project_name"));
      comment.setProjectVersion(rs.getString("project_version"));
      comment.setReviewStatus(rs.getString("review_status_id"));
      comment.setStableId(rs.getString("stable_id"));
      comment.setOrganism(rs.getString("organism"));
      comment.setUserName(rs.getString("user_name"));
      comment.setOrganization(rs.getString("organization"));

      // get clob content
      comment.setContent(_commentDb.getPlatform().getClobData(rs, "content"));

      // load locations
      loadLocations(commentId, comment);

      // load external databases
      loadExternalDbs(commentId, comment);

      // load pubmed ids
      loadReference(commentId, comment, "pubmed");

      // load author names
      loadAuthor(commentId, comment, "author");

      // load genbank ids
      loadReference(commentId, comment, "genbank");

      // load doi ids
      loadReference(commentId, comment, "doi");

      // load files
      loadFiles(commentId, comment);

      // load associated stable ids
      loadStableIds(commentId, comment);

      // load target category names
      loadTargetCategoryNames(commentId, comment);

      // phenotype data
      if (rs.getString("comment_target_id").equals("phenotype")) {
        loadMutantMarkerNames(commentId, comment);
        loadMutantReporterNames(commentId, comment);
        loadPhenotype(commentId, comment);
      }

      return comment;
    }
    catch (SQLException ex) {
      ex.printStackTrace();
      throw new WdkModelException(ex);
    }
    finally {
      SqlUtils.closeResultSetAndStatement(rs, ps);

      // print connection status
      printStatus();
    }
  }

  private void loadReference(long commentId, Comment comment, String databaseName) throws WdkModelException {
    StringBuffer sql = new StringBuffer();
    sql.append("SELECT source_id ");
    sql.append(" FROM ");
    sql.append(_config.getCommentSchema() + "commentReference ");
    sql.append("WHERE comment_id = ? and database_name = ?");

    ResultSet rs = null;
    PreparedStatement ps = null;
    try {
      ps = SqlUtils.getPreparedStatement(_commentDs, sql.toString());
      ps.setLong(1, commentId);
      ps.setString(2, databaseName);
      rs = ps.executeQuery();

      ArrayList<String> ids = new ArrayList<String>();
      while (rs.next()) {
        String sourceId = rs.getString("source_id");
        ids.add(sourceId);
      }
      if (ids.size() > 0) {
        comment.addReference(ids.toArray(new String[ids.size()]), databaseName);
      }
    }
    catch (SQLException e) {
      throw new WdkModelException(e);
    }
    finally {
      SqlUtils.closeResultSetAndStatement(rs, ps);
    }
  }

  private void loadAuthor(long commentId, Comment comment, String databaseName) throws WdkModelException {
    StringBuffer sql = new StringBuffer();
    sql.append("SELECT source_id ");
    sql.append(" FROM ");
    sql.append(_config.getCommentSchema() + "commentReference ");
    sql.append("WHERE comment_id = ? and database_name = ?");

    ResultSet rs = null;
    PreparedStatement ps = null;
    try {
      ps = SqlUtils.getPreparedStatement(_commentDs, sql.toString());
      ps.setLong(1, commentId);
      ps.setString(2, databaseName);
      rs = ps.executeQuery();

      ArrayList<String> ids = new ArrayList<String>();
      while (rs.next()) {
        String sourceId = rs.getString("source_id");
        ids.add(sourceId);
      }
      if (ids.size() > 0) {
        comment.addReference(ids.toArray(new String[ids.size()]), databaseName);
      }
    }
    catch (SQLException e) {
      throw new WdkModelException(e);
    }
    finally {
      SqlUtils.closeResultSetAndStatement(rs, ps);
    }
  }

  private void loadTargetCategoryNames(long commentId, Comment comment) throws WdkModelException {
    StringBuffer sql = new StringBuffer();
    sql.append("SELECT b.category, b.target_category_id");
    sql.append(" FROM ");
    sql.append(_config.getCommentSchema() + "commentTargetCategory a, ");
    sql.append(_config.getCommentSchema() + "targetCategory b ");
    sql.append("WHERE a.target_category_id = b.target_category_id AND a.comment_id = ?");

    ResultSet rs = null;
    PreparedStatement ps = null;
    try {
      ps = SqlUtils.getPreparedStatement(_commentDs, sql.toString());
      ps.setLong(1, commentId);
      rs = ps.executeQuery();

      ArrayList<String> names = new ArrayList<String>();
      ArrayList<Long> ids = new ArrayList<>();
      while (rs.next()) {
        String category = rs.getString("category");
        long categoryId = rs.getLong("target_category_id");
        names.add(category);
        ids.add(categoryId);
      }
      if (names.size() > 0) {
        comment.addTargetCategoryNames(names.toArray(new String[names.size()]));

        long[] tid = new long[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
          tid[i] = ids.get(i).longValue();
        }
        comment.setTargetCategoryIds(tid);
      }
    }
    catch (SQLException e) {
      throw new WdkModelException(e);
    }
    finally {
      SqlUtils.closeResultSetAndStatement(rs, ps);
    }
  }

  private void loadMutantMarkerNames(long commentId, Comment comment) throws WdkModelException {
    StringBuffer sql = new StringBuffer();
    sql.append("SELECT b.mutant_marker ");
    sql.append(" FROM ");
    sql.append(_config.getCommentSchema() + "PhenotypeMutantMarker a, ");
    sql.append(_config.getCommentSchema() + "MutantMarker b ");
    sql.append("WHERE a.mutant_marker_id = b.mutant_marker_id AND a.comment_id = ?");

    ResultSet rs = null;
    PreparedStatement ps = null;
    try {
      ps = SqlUtils.getPreparedStatement(_commentDs, sql.toString());
      ps.setLong(1, commentId);
      rs = ps.executeQuery();

      ArrayList<String> ids = new ArrayList<String>();
      while (rs.next()) {
        String mutant_marker = rs.getString("mutant_marker");
        ids.add(mutant_marker);
      }
      if (ids.size() > 0) {
        comment.addMutantMarkerNames(ids.toArray(new String[ids.size()]));
      }
    }
    catch (SQLException e) {
      throw new WdkModelException(e);
    }
    finally {
      SqlUtils.closeResultSetAndStatement(rs, ps);
    }
  }

  private void loadMutantReporterNames(long commentId, Comment comment) throws WdkModelException {
    StringBuffer sql = new StringBuffer();
    sql.append("SELECT b.mutant_reporter ");
    sql.append(" FROM ");
    sql.append(_config.getCommentSchema() + "PhenotypeMutantReporter a, ");
    sql.append(_config.getCommentSchema() + "MutantReporter b ");
    sql.append("WHERE a.mutant_reporter_id = b.mutant_reporter_id AND a.comment_id = ?");

    ResultSet rs = null;
    PreparedStatement ps = null;
    try {
      ps = SqlUtils.getPreparedStatement(_commentDs, sql.toString());
      ps.setLong(1, commentId);
      rs = ps.executeQuery();

      ArrayList<String> ids = new ArrayList<String>();
      while (rs.next()) {
        String mutant_reporter = rs.getString("mutant_reporter");
        ids.add(mutant_reporter);
      }
      if (ids.size() > 0) {
        comment.setMutantReporterNames(ids.toArray(new String[ids.size()]));
      }
    }
    catch (SQLException e) {
      throw new WdkModelException(e);
    }
    finally {
      SqlUtils.closeResultSetAndStatement(rs, ps);
    }
  }

  private void loadPhenotype(long commentId, Comment comment) throws WdkModelException {
    StringBuffer sql = new StringBuffer();
    sql.append("SELECT a.background, a.mutant_description, ");
    sql.append("a.phenotype_description, ");
    sql.append("c.mutant_method, ");
    sql.append("d.mutant_status, ");
    sql.append("e.mutant_expression, ");
    sql.append("f.mutant_category_name, ");
    sql.append("g.phenotype_loc, ");
    sql.append("b.mutant_type ");
    sql.append(" FROM ");
    sql.append(_config.getCommentSchema() + "Phenotype a, ");
    sql.append(_config.getCommentSchema() + "MutantType b, ");
    sql.append(_config.getCommentSchema() + "MutantMethod c, ");
    sql.append(_config.getCommentSchema() + "MutantStatus d, ");
    sql.append(_config.getCommentSchema() + "MutantExpression e, ");
    sql.append("(SELECT pmc.comment_id, ");
    sql.append("apidb.tab_to_string(set(cast(collect(mc.mutant_category) as apidb.varchartab)),'; ') as mutant_category_name ");
    sql.append(" FROM ");
    sql.append(_config.getCommentSchema() + "MutantCategory mc, ");
    sql.append(_config.getCommentSchema() + "PhenotypeMutantCategory pmc ");
    sql.append("WHERE mc.mutant_category_id = pmc.mutant_category_id ");
    sql.append("GROUP BY pmc.comment_id) f, ");
    sql.append(_config.getCommentSchema() + "PhenotypeLoc g ");
    sql.append("WHERE a.mutant_type_id = b.mutant_type_id ");
    sql.append("AND a.mutant_method_id = c.mutant_method_id(+) ");
    sql.append("AND a.mutant_status_id = d.mutant_status_id(+) ");
    sql.append("AND a.mutant_expression_id = e.mutant_expression_id(+) ");
    sql.append("AND a.phenotype_loc_id = g.phenotype_loc_id(+) ");
    sql.append("AND a.comment_id = ?");

    ResultSet rs = null;
    PreparedStatement ps = null;
    try {
      ps = SqlUtils.getPreparedStatement(_commentDs, sql.toString());
      ps.setLong(1, commentId);
      rs = ps.executeQuery();

      while (rs.next()) {

        comment.setBackground(rs.getString("background"));
        comment.setMutantTypeName(rs.getString("mutant_type"));
        comment.setMutantStatusName(rs.getString("mutant_status"));
        comment.setMutationMethodName(rs.getString("mutant_method"));
        comment.setMutationDescription(rs.getString("mutant_description"));
        comment.setMutantExpressionName(rs.getString("mutant_expression"));
        comment.setMutantCategoryName(rs.getString("mutant_category_name"));
        comment.setPhenotypeLocName(rs.getString("phenotype_loc"));
      }

    }
    catch (SQLException e) {
      throw new WdkModelException(e);
    }
    finally {
      SqlUtils.closeResultSetAndStatement(rs, ps);
    }
  }

  private void loadFiles(long commentId, Comment comment) throws WdkModelException {
    StringBuffer sql = new StringBuffer();
    sql.append("SELECT file_id, name, notes ");
    sql.append(" FROM ");
    sql.append(_config.getCommentSchema() + "commentFile ");
    sql.append("WHERE comment_id = ?");

    ResultSet rs = null;
    PreparedStatement ps = null;
    try {
      ps = SqlUtils.getPreparedStatement(_commentDs, sql.toString());
      ps.setLong(1, commentId);
      rs = ps.executeQuery();

      ArrayList<String> ids = new ArrayList<String>();
      while (rs.next()) {
        long file_id = rs.getLong("file_id");
        String name = rs.getString("name");
        String notes = rs.getString("notes");
        ids.add(file_id + "|" + name + "|" + notes);
      }
      if (ids.size() > 0) {
        comment.addFiles(ids.toArray(new String[ids.size()]));
      }
    }
    catch (SQLException e) {
      throw new WdkModelException(e);
    }
    finally {
      SqlUtils.closeResultSetAndStatement(rs, ps);
    }
  }

  private void loadStableIds(long commentId, Comment comment) throws WdkModelException {
    StringBuffer sql = new StringBuffer();
    sql.append("SELECT stable_id ");
    sql.append(" FROM ");
    sql.append(_config.getCommentSchema() + "CommentStableId ");
    sql.append("WHERE comment_id = ?");

    ResultSet rs = null;
    PreparedStatement ps = null;
    try {
      ps = SqlUtils.getPreparedStatement(_commentDs, sql.toString());
      ps.setLong(1, commentId);
      rs = ps.executeQuery();

      ArrayList<String> ids = new ArrayList<String>();
      while (rs.next()) {
        String stable_id = rs.getString("stable_id");
        ids.add(stable_id);
      }
      if (ids.size() > 0) {
        comment.addAssociatedStableIds(ids.toArray(new String[ids.size()]));
      }
    }
    catch (SQLException e) {
      throw new WdkModelException(e);
    }
    finally {
      SqlUtils.closeResultSetAndStatement(rs, ps);
    }
  }

  private void loadLocations(long commentId, Comment comment) throws WdkModelException {
    StringBuffer sql = new StringBuffer();
    sql.append("SELECT location_start, location_end, is_reverse, ");
    sql.append("coordinate_type FROM ");
    sql.append(_config.getCommentSchema() + "locations ");
    sql.append("WHERE comment_id = ?");

    ResultSet rs = null;
    PreparedStatement ps = null;
    try {
      ps = SqlUtils.getPreparedStatement(_commentDs, sql.toString());
      ps.setLong(1, commentId);
      rs = ps.executeQuery();

      while (rs.next()) {
        long start = rs.getLong("location_start");
        long end = rs.getLong("location_end");
        boolean reversed = rs.getBoolean("is_reverse");
        String coordinateType = rs.getString("coordinate_type");
        comment.addLocation(reversed, start, end, coordinateType);
      }
    }
    catch (SQLException e) {
      throw new WdkModelException(e);
    }
    finally {
      SqlUtils.closeResultSetAndStatement(rs, ps);
    }
  }

  private void loadExternalDbs(long commentId, Comment comment) throws WdkModelException {
    String commentSchema = _config.getCommentSchema();

    StringBuffer sql = new StringBuffer();
    sql.append("SELECT ed.external_database_name, ed.external_database_version ");
    sql.append("FROM " + commentSchema + "external_databases ed, ");
    sql.append(commentSchema + "comment_external_database ced ");
    sql.append("WHERE ced.comment_id = ?");
    sql.append(" AND ced.external_database_id = ed.external_database_id");

    ResultSet rs = null;
    PreparedStatement ps = null;
    try {
      ps = SqlUtils.getPreparedStatement(_commentDs, sql.toString());
      ps.setLong(1, commentId);
      rs = ps.executeQuery();

      while (rs.next()) {
        String externalDbName = rs.getString("external_database_name");
        String externalDbVersion = rs.getString("external_database_version");
        comment.addExternalDatabase(externalDbName, externalDbVersion);
      }
    }
    catch (SQLException e) {
      throw new WdkModelException(e);
    }
    finally {
      SqlUtils.closeResultSetAndStatement(rs, ps);
    }
  }

  public Comment[] queryComments(Long userId, String projectName, String stableId, String conceptual,
      String reviewStatus, String keyword, String commentTargetId) throws WdkModelException {

    StringBuffer where = new StringBuffer();
    if (userId != null) {
      where.append(" c.user_id = '" + userId + "'");
    }
    if (projectName != null) {
      projectName = projectName.replace('*', '%');
      projectName = projectName.replaceAll("'", "");
      if (where.length() > 0)
        where.append(" AND");
      where.append(" c.project_name like '" + projectName + "'");
    }
    if (stableId != null) {
      stableId = stableId.replace('*', '%');
      stableId = stableId.replaceAll("'", "");
      if (where.length() > 0)
        where.append(" AND ");
      where.append(" (c.stable_id like '" + stableId + "'");
      where.append(" OR d.stable_id like '" + stableId + "') ");
    }
    if (conceptual != null) {
      boolean concpt = Boolean.parseBoolean(conceptual);
      if (where.length() > 0)
        where.append(" AND ");
      where.append(" c.conceptual like " + concpt);
    }
    if (reviewStatus != null) {
      reviewStatus = stableId.replace('*', '%');
      reviewStatus = stableId.replaceAll("'", "");
      if (where.length() > 0)
        where.append(" AND ");
      where.append(" c.review_status like '" + reviewStatus + "'");
    }
    if (keyword != null) {
      keyword = stableId.replace('*', '%');
      keyword = stableId.replaceAll("'", "");
      if (where.length() > 0)
        where.append(" AND ");
      where.append(" (c.headline like '" + keyword);
      where.append("' OR c.content like '" + keyword + "')");
    }
    if (commentTargetId != null) {
      if (where.length() > 0)
        where.append(" AND ");
      where.append(" c.comment_target_id = '" + commentTargetId + "'");
    }

    StringBuffer sql = new StringBuffer();
    sql.append("SELECT distinct * FROM ( ");
    sql.append("SELECT c.comment_id FROM ");
    sql.append(_config.getCommentSchema() + "comments c, ");
    sql.append(_config.getCommentSchema() + "commentStableId d, ");
    sql.append(_config.getCommentSchema() + "comment_users u ");
    sql.append("WHERE c.user_id = u.user_id ");
    sql.append("AND c.is_visible = 1 ");
    sql.append("AND c.comment_id = d.comment_id(+) ");
    if (where.length() > 0)
      sql.append(" AND " + where.toString());
    sql.append(" ) ORDER BY comment_id DESC");

    List<Comment> comments = new ArrayList<Comment>();
    ResultSet rs = null;
    try {
      rs = SqlUtils.executeQuery(_commentDs, sql.toString(), "api-comment-select-comment");
      while (rs.next()) {
        long commentId = rs.getLong("comment_id");
        Comment comment = getComment(commentId);
        comments.add(comment);
      }
    }
    catch (SQLException ex) {
      throw new WdkModelException(ex);
    }
    finally {
      SqlUtils.closeResultSetAndStatement(rs, null);

      // print connection status
      printStatus();
    }
    Comment[] array = new Comment[comments.size()];
    comments.toArray(array);

    return array;
  }

  public void deleteComment(String commentId) throws WdkModelException {
    String commentSchema = _config.getCommentSchema();

    try {
      // update comments table set is_visible = 0
      String sql = "UPDATE " + commentSchema + "comments " + "SET is_visible = 0 " + "WHERE comment_id = " + commentId;
      SqlUtils.executeUpdate(_commentDs, sql, "wdk-comment-hide-comment");

    }
    catch (SQLException e) {
      throw new WdkModelException(e);
    }

  }

  public ArrayList<MultiBox> getMultiBoxData(String nameCol, String valueCol, String table, String condition) {

    ArrayList<MultiBox> list = new ArrayList<MultiBox>();
    ResultSet rs = null;
    PreparedStatement ps = null;
    
    StringBuffer sql = new StringBuffer();
    sql.append("SELECT " + nameCol + "," + valueCol);
    sql.append(" FROM  " + _config.getCommentSchema() + table);
    if (condition != null) {
      sql.append(" WHERE " + condition);
    }

    MultiBox multiBox = null;

    try {
      ps = SqlUtils.getPreparedStatement(_commentDs, sql.toString());
      rs = ps.executeQuery();

      while (rs.next()) {
        String name = rs.getString(nameCol);
        int value = rs.getInt(valueCol);
        multiBox = new MultiBox(name, value + "");
        list.add(multiBox);
      }
      return list;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    finally {
      SqlUtils.closeResultSetAndStatement(rs, ps);
      // printStatus();
    }
  }

  public CommentConfig getCommentConfig() {
    return _config;
  }

  private void printStatus() {
    int active = _commentDb.getActiveCount();
    int idle = _commentDb.getIdleCount();
    LOG.info("Comment connections: active=" + active + ", idle=" + idle);
  }

  public DataSource getCommentDataSource() {
    return _commentDs;
  }

  public void close() throws Exception {
    if (!_isReusingUserDb) {
      _commentDb.close();
    }
  }
}
