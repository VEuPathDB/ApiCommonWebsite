/**
 * 
 */
package org.apidb.apicommon.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.dbms.DBPlatform;
import org.gusdb.wdk.model.dbms.SqlUtils;

/**
 * @author xingao
 * 
 */
public class CommentFactory {

    private static CommentFactory factory;

    private Logger logger = Logger.getLogger(CommentFactory.class);
    private DBPlatform platform;
    private CommentConfig config;

    public static CommentFactory getInstance(String gusHome, String projectId) {
        if (factory == null) initialize(gusHome, projectId);
        return factory;
    }

    private static void initialize(String gusHome, String projectId) {
        try {
            WdkModel wdkModel = WdkModel.construct(projectId, gusHome);

            // parse and load the configuration
            CommentConfigParser parser = new CommentConfigParser(gusHome);
            CommentConfig config = parser.parseConfig(projectId);

            // create a platform object
            DBPlatform platform = (DBPlatform) Class.forName(
                    config.getPlatformClass()).newInstance();
            platform.initialize(wdkModel, "Comment", config);

            // create a factory instance
            factory = new CommentFactory(platform, config);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private CommentFactory(DBPlatform platform, CommentConfig config) {
        this.platform = platform;
        this.config = config;
    }

    public DBPlatform getCommentPlatform() {
        return platform;
    }

    public CommentTarget getCommentTarget(String internalValue)
            throws WdkModelException {

        // load the comment target definition of the given internal value

        ResultSet rs = null;
        CommentTarget target = null;
        String query = null;
        try {
            query = "SELECT * FROM " + config.getCommentSchema()
                    + "comment_target WHERE comment_target_id=?";
            PreparedStatement ps = SqlUtils.getPreparedStatement(
                    platform.getDataSource(), query);
            ps.setString(1, internalValue);
            rs = ps.executeQuery();
            if (!rs.next())
                throw new WdkModelException("The comment target cannot be "
                        + "found with the given internal value: "
                        + internalValue);

            target = new CommentTarget(internalValue);
            target.setDisplayName(rs.getString("comment_target_name"));
            target.setRequireLocation((rs.getInt("require_location") != 0));
        } catch (SQLException ex) {
            throw new WdkModelException(ex);
        } finally {
            // close the connection
            SqlUtils.closeResultSet(rs);
        }
        return target;
    }

    public void addComment(Comment comment) throws WdkModelException {
        String commentSchema = config.getCommentSchema();

        PreparedStatement ps = null;
        // get a new comment id
        try {
            // DataSource dataSource = platform.getDataSource();
            int commentId = platform.getNextId(commentSchema, "comments");
            int[] targetCategoryIds = comment.getTargetCategoryIds();
            String[] pmIds = comment.getPmIds();
            String[] accessions = comment.getAccessions();
            String[] files = comment.getFiles();
            String[] associatedStableIds = comment.getAssociatedStableIds();

            ps = SqlUtils.getPreparedStatement(
                    platform.getDataSource(),
                    "INSERT INTO "
                            + commentSchema
                            + "comments (comment_id, "
                            + "email, comment_date, comment_target_id, "
                            + "stable_id, conceptual, project_name, "
                            + "project_version, headline, content, "
                            + "location_string, review_status_id, organism, user_id) "
                            + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

            long currentMillis = System.currentTimeMillis();

            ps.setInt(1, commentId);
            ps.setString(2, comment.getEmail());
            ps.setTimestamp(3, new Timestamp(currentMillis));
            ps.setString(4, comment.getCommentTarget());
            ps.setString(5, comment.getStableId());
            ps.setBoolean(6, comment.isConceptual());
            ps.setString(7, comment.getProjectName());
            ps.setString(8, comment.getProjectVersion());
            ps.setString(9, comment.getHeadline());
            platform.setClobData(ps, 10, comment.getContent(), false);
            ps.setString(11, comment.getLocationString());
            String reviewStatus = (comment.getReviewStatus() != null && comment.getReviewStatus().length() > 0)
                    ? comment.getReviewStatus()
                    : Comment.COMMENT_REVIEW_STATUS_UNKNOWN;
            ps.setString(12, reviewStatus);
            ps.setString(13, comment.getOrganism());
            ps.setInt(14, comment.getUserId());

            int result = ps.executeUpdate();
            logger.debug("Inserted comment row: " + result);

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

            if ((accessions != null) && (accessions.length > 0)) {
                saveAccessions(commentId, accessions);
            }

            if ((files != null) && (files.length > 0)) {
                saveFiles(commentId, files);
            }

            if ((associatedStableIds != null)
                    && (associatedStableIds.length > 0)) {
                saveAssociatedStableIds(commentId, associatedStableIds);
            }

            if (comment.getCommentTarget().equalsIgnoreCase("phenotype")) {
                savePhenotype(commentId, comment.getBackground(),
                        comment.getMutantStatus(), comment.getMutationType(),
                        comment.getMutationMethod(),
                        comment.getMutantExpression(),
                        comment.getPhenotypeLoc(), comment.getContent());
                saveMutantMarkers(commentId, comment.getMutantMarkers());
                saveMutantReporters(commentId, comment.getMutantReporters());
                savePhenotypeCategory(commentId, comment.getPhenotypeCategory());
            }

            // get a new comment in order to fetch the user info
            Comment newComment = getComment(commentId);

            comment.setUserName(newComment.getUserName());
            comment.setOrganization(newComment.getOrganization());
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new WdkModelException(ex);
        } finally {
            SqlUtils.closeStatement(ps);
        }

        // print connection status
        printStatus();
    }

    private void saveLocations(int commentId, Comment comment)
            throws SQLException, org.gusdb.wdk.model.WdkModelException {
        String commentSchema = config.getCommentSchema();

        // construct sql
        StringBuffer sql = new StringBuffer();
        sql.append("INSERT INTO " + commentSchema + "locations (comment_id, ");
        sql.append("location_id, location_start, location_end, is_reverse, ");
        sql.append("coordinate_type) VALUES (?, ?, ?, ?, ?, ?)");
        PreparedStatement statement = null;
        try {
            statement = SqlUtils.getPreparedStatement(platform.getDataSource(),
                    sql.toString());

            Location[] locations = comment.getLocations();
            for (Location location : locations) {
                int locationId = platform.getNextId(commentSchema, "locations");
                statement.setInt(1, commentId);
                statement.setInt(2, locationId);
                statement.setLong(3, location.getLocationStart());
                statement.setLong(4, location.getLocationEnd());
                statement.setBoolean(5, location.isReversed());
                statement.setString(6, location.getCoordinateType());
                statement.execute();
            }
        } finally {
            SqlUtils.closeStatement(statement);
        }
    }

    private void savePhenotype(int commentId, String background,
            int mutantStatus, int mutationType, int mutationMethod,
            int mutantExpression, int phenotypeLoc, String phenotypeDescription)
            throws SQLException, WdkModelException {

        String commentSchema = config.getCommentSchema();

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
            statement = SqlUtils.getPreparedStatement(platform.getDataSource(),
                    sql.toString());

            statement.setInt(1, platform.getNextId(commentSchema, "phenotype"));
            statement.setInt(2, commentId);
            statement.setString(3, background);
            statement.setInt(4, mutantStatus);
            statement.setInt(5, mutationType);
            statement.setInt(6, mutationMethod);
            statement.setInt(7, mutantExpression);
            statement.setInt(8, phenotypeLoc);
            statement.setString(9, phenotypeDescription);
            statement.execute();
        } finally {
            SqlUtils.closeStatement(statement);
        }
    }

    private void saveMutantMarkers(int commentId, int[] mutantMarkers)
            throws SQLException, WdkModelException {
        String commentSchema = config.getCommentSchema();

        StringBuffer sql = new StringBuffer();
        sql.append("INSERT INTO " + commentSchema + "PhenotypeMutantMarker ");
        sql.append("(comment_mutant_marker_id, comment_id, ");
        sql.append("mutant_marker_id ");
        sql.append(") VALUES (?, ?, ?)");
        PreparedStatement statement = null;
        try {
            statement = SqlUtils.getPreparedStatement(platform.getDataSource(),
                    sql.toString());

            for (int mutantMarker : mutantMarkers) {
                statement.setInt(1, platform.getNextId(commentSchema,
                        "commentMutantMarker"));
                statement.setInt(2, commentId);
                statement.setInt(3, mutantMarker);
                statement.execute();
            }
        } finally {
            SqlUtils.closeStatement(statement);
        }
    }

    private void saveMutantReporters(int commentId, int[] mutantReporters)
            throws SQLException, WdkModelException {
        String commentSchema = config.getCommentSchema();

        StringBuffer sql = new StringBuffer();
        sql.append("INSERT INTO " + commentSchema + "PhenotypeMutantReporter ");
        sql.append("(comment_mutant_reporter_id, comment_id, ");
        sql.append("mutant_reporter_id ");
        sql.append(") VALUES (?, ?, ?)");
        PreparedStatement statement = null;
        try {
            statement = SqlUtils.getPreparedStatement(platform.getDataSource(),
                    sql.toString());

            for (int mutantReporter : mutantReporters) {
                statement.setInt(1, platform.getNextId(commentSchema,
                        "commentMutantReporter"));
                statement.setInt(2, commentId);
                statement.setInt(3, mutantReporter);
                statement.execute();
            }
        } finally {
            SqlUtils.closeStatement(statement);
        }
    }

    private void savePhenotypeCategory(int commentId, int[] phenotypeCategory)
            throws SQLException, WdkModelException {
        String commentSchema = config.getCommentSchema();

        StringBuffer sql = new StringBuffer();
        sql.append("INSERT INTO " + commentSchema + "PhenotypeMutantCategory ");
        sql.append("(comment_mutant_category_id, comment_id, ");
        sql.append("mutant_category_id ");
        sql.append(") VALUES (?, ?, ?)");
        PreparedStatement statement = null;
        try {
            statement = SqlUtils.getPreparedStatement(platform.getDataSource(),
                    sql.toString());

            for (int cat : phenotypeCategory) {
                statement.setInt(1, platform.getNextId(commentSchema,
                        "phenotypeMutantCategory"));
                statement.setInt(2, commentId);
                statement.setInt(3, cat);
                statement.execute();
            }
        } finally {
            SqlUtils.closeStatement(statement);
        }
    }

    private void saveCommentTargetCategory(int commentId,
            int[] targetCategoryIds) throws SQLException, WdkModelException {
        String commentSchema = config.getCommentSchema();

        // construct sql
        StringBuffer sql = new StringBuffer();
        sql.append("INSERT INTO " + commentSchema + "CommentTargetCategory ");
        sql.append("(comment_target_category_id, comment_id, ");
        sql.append("target_category_id ");
        sql.append(") VALUES (?, ?, ?)");
        PreparedStatement statement = null;
        try {
            statement = SqlUtils.getPreparedStatement(platform.getDataSource(),
                    sql.toString());

            for (int targetCategoryId : targetCategoryIds) {
                statement.setInt(1, platform.getNextId(commentSchema,
                        "commentTargetCategory"));
                statement.setInt(2, commentId);
                statement.setInt(3, targetCategoryId);
                statement.execute();
            }
        } finally {
            SqlUtils.closeStatement(statement);
        }
    }

    private void savePmIds(int commentId, String[] pmIds) throws SQLException,
            WdkModelException {
        String commentSchema = config.getCommentSchema();

        // construct sql
        StringBuffer sql = new StringBuffer();
        sql.append("INSERT INTO " + commentSchema + "CommentReference ");
        sql.append("(comment_reference_id, source_id, ");
        sql.append("database_name, comment_id ");
        sql.append(") VALUES (?, ?, ?, ?)");
        PreparedStatement statement = null;
        try {
            statement = SqlUtils.getPreparedStatement(platform.getDataSource(),
                    sql.toString());

            for (String pmId : pmIds) {
                if ((pmId != null) && (pmId.trim().length() != 0)) {
                    int commentPmId = platform.getNextId(commentSchema,
                            "commentReference");
                    statement.setInt(1, commentPmId);
                    statement.setString(2, pmId);
                    statement.setString(3, "pubmed");
                    statement.setInt(4, commentId);
                    statement.execute();
                }
            }
        } finally {
            SqlUtils.closeStatement(statement);
        }
    }

    private void saveAccessions(int commentId, String[] accessions)
            throws SQLException, WdkModelException {
        String commentSchema = config.getCommentSchema();

        // construct sql
        StringBuffer sql = new StringBuffer();
        sql.append("INSERT INTO " + commentSchema + "CommentReference ");
        sql.append("(comment_reference_id, source_id, ");
        sql.append("database_name, comment_id ");
        sql.append(") VALUES (?, ?, ?, ?)");
        PreparedStatement statement = null;
        try {
            statement = SqlUtils.getPreparedStatement(platform.getDataSource(),
                    sql.toString());

            for (String accession : accessions) {
                if ((accession != null) && (accession.trim().length() != 0)) {
                    int commentPmId = platform.getNextId(commentSchema,
                            "commentReference");
                    statement.setInt(1, commentPmId);
                    statement.setString(2, accession);
                    statement.setString(3, "genbank");
                    statement.setInt(4, commentId);
                    statement.execute();
                }
            }
        } finally {
            SqlUtils.closeStatement(statement);
        }
    }

    private void saveFiles(int commentId, String[] files) throws SQLException,
            WdkModelException {
        String commentSchema = config.getCommentSchema();

        // construct sql
        StringBuffer sql = new StringBuffer();
        sql.append("INSERT INTO " + commentSchema + "CommentFile ");
        sql.append("(file_id, name, notes, ");
        sql.append(" comment_id ");
        sql.append(") VALUES (?, ?, ?, ?)");
        PreparedStatement statement = null;
        try {
            statement = SqlUtils.getPreparedStatement(platform.getDataSource(),
                    sql.toString());

            for (String file : files) {
                if (file == null) continue;
                String[] str = file.split("\\|");
                statement.setInt(1, Integer.parseInt(str[0]));
                statement.setString(2, str[1]);
                statement.setString(3, str[2]);
                statement.setInt(4, commentId);
                statement.execute();
            }
        } finally {
            SqlUtils.closeStatement(statement);
        }
    }

    private void saveAssociatedStableIds(int commentId,
            String[] associatedStableIds) throws SQLException,
            WdkModelException {
        String commentSchema = config.getCommentSchema();

        // construct sql
        StringBuffer sql = new StringBuffer();
        sql.append("INSERT INTO " + commentSchema + "CommentStableId ");
        sql.append("(comment_stable_id, stable_id, ");
        sql.append(" comment_id ");
        sql.append(") VALUES (?, ?, ?)");
        PreparedStatement statement = null;
        try {
            statement = SqlUtils.getPreparedStatement(platform.getDataSource(),
                    sql.toString());

            for (String associatedStableId : associatedStableIds) {
                if ((associatedStableId != null)
                        && (associatedStableId.trim().length() != 0)) {
                    int stableId = platform.getNextId(commentSchema,
                            "commentStableId");
                    statement.setInt(1, stableId);
                    statement.setString(2, associatedStableId);
                    statement.setInt(3, commentId);
                    statement.execute();
                }
            }
        } finally {
            SqlUtils.closeStatement(statement);
        }
    }

    /**
     * For the first release, this method hard-code the external database name
     * and version, instead of looking up directly in GUS. This avoids the
     * backward dblink requirement; The site should provide such information to
     * the comment factory in future releases.
     * 
     * UPDATE: Assume that the Comment parameter already has the external
     * database info filled in. Use the first extdb/versions (Is there a case at
     * all where we need more than one ext db???)
     * 
     * @param commentId
     * @param comment
     * @throws SQLException
     * @throws WdkModelException
     */
    private void saveExternalDbs(int commentId, Comment comment)
            throws SQLException, WdkModelException {
        String commentSchema = config.getCommentSchema();
        // String dblink = config.getProjectDbLink();
        // String stableId = comment.getStableId();
        DataSource dataSource = platform.getDataSource();

        // construct sqls
        StringBuffer sqlQueryDb = new StringBuffer();
        sqlQueryDb.append("SELECT external_database_id ");
        sqlQueryDb.append("FROM " + commentSchema + "external_databases ");
        sqlQueryDb.append("WHERE external_database_name = ? ");
        sqlQueryDb.append("AND external_database_version = ?");

        StringBuffer sqlInsertDb = new StringBuffer();
        sqlInsertDb.append("INSERT INTO " + commentSchema
                + "external_databases ");
        sqlInsertDb.append("(external_database_id, external_database_name, ");
        sqlInsertDb.append("external_database_version) VALUES (?, ?, ?)");

        StringBuffer sqlInsertLink = new StringBuffer();
        sqlInsertLink.append("INSERT INTO " + commentSchema
                + "comment_external_database ");
        sqlInsertLink.append("(external_database_id, comment_id) ");
        sqlInsertLink.append("VALUES (?, ?)");

        PreparedStatement psQUeryDb = null, psInsertDb = null, psInsertLink = null;
        try {
            // construct prepared statements
            psQUeryDb = SqlUtils.getPreparedStatement(dataSource,
                    sqlQueryDb.toString());

            psInsertDb = SqlUtils.getPreparedStatement(dataSource,
                    sqlInsertDb.toString());

            psInsertLink = SqlUtils.getPreparedStatement(dataSource,
                    sqlInsertLink.toString());

            // add every external database record into the database
            for (ExternalDatabase externalDb : comment.getExternalDbs()) {
                psQUeryDb.setString(1, externalDb.getExternalDbName());
                psQUeryDb.setString(2, externalDb.getExternalDbVersion());
                ResultSet rsQueryDb = null;
                int externalDbId;
                try {
                    rsQueryDb = psQUeryDb.executeQuery();
                    if (!rsQueryDb.next()) {
                        // external database entry doesn't exist
                        externalDbId = platform.getNextId(commentSchema,
                                "external_databases");
                        psInsertDb.setInt(1, externalDbId);
                        psInsertDb.setString(2, externalDb.getExternalDbName());
                        psInsertDb.setString(3,
                                externalDb.getExternalDbVersion());
                        psInsertDb.execute();
                    } else { // has entry, get the external_database_id
                        externalDbId = rsQueryDb.getInt("external_database_id");
                    }
                } finally {
                    // the ResultSet is closed manually, instead of using
                    // SqlUtilssince we need to keep the underline statement
                    // open during the for loop
                    rsQueryDb.close();
                }

                // add the reference link
                psInsertLink.setInt(1, externalDbId);
                psInsertLink.setInt(2, commentId);
                psInsertLink.execute();
            }
        } finally {
            // close statements and result set
            SqlUtils.closeStatement(psQUeryDb);
            SqlUtils.closeStatement(psInsertDb);
            SqlUtils.closeStatement(psInsertLink);
        }
    }

    public Comment getComment(int commentId) throws WdkModelException {
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT c.email, c.comment_date, c.comment_target_id, ");
        sql.append("c.conceptual, c.headline, c.project_name, ");
        sql.append("c.project_version, c.review_status_id, c.stable_id, ");
        sql.append("c.content, ");
        sql.append("substr(c.organism, 1, instr(c.organism || '  ', ' ', 1, 2)-1) as organism, ");
        sql.append("u.first_name || ' ' || u.last_name || ', ' || u.title  as user_name, ");
        sql.append("u.organization, c.content FROM ");
        sql.append(config.getCommentSchema() + "comments c, ");
        sql.append(config.getUserLoginSchema() + "users"
                + config.getUserLoginDbLink() + " u ");
        sql.append("WHERE c.user_id = u.user_id ");
        sql.append("AND c.comment_id = ? ");

        ResultSet rs = null;
        try {
            PreparedStatement ps = SqlUtils.getPreparedStatement(
                    platform.getDataSource(), sql.toString());
            ps.setInt(1, commentId);
            rs = ps.executeQuery();

            if (!rs.next())
                throw new WdkModelException("Comment of the given id '"
                        + commentId + "' cannot be found.");

            // construct a comment object
            Comment comment = new Comment(rs.getString("email"));
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
            comment.setContent(platform.getClobData(rs, "content"));

            // load locations
            loadLocations(commentId, comment);

            // load external databases
            loadExternalDbs(commentId, comment);

            // load pubmed ids
            loadReference(commentId, comment, "pubmed");

            // load genbank ids
            loadReference(commentId, comment, "genbank");

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
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new WdkModelException(ex);
        } finally {
            SqlUtils.closeResultSet(rs);

            // print connection status
            printStatus();
        }
    }

    private void loadReference(int commentId, Comment comment,
            String databaseName) throws SQLException {
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT source_id ");
        sql.append(" FROM ");
        sql.append(config.getCommentSchema() + "commentReference ");
        sql.append("WHERE comment_id = ? and database_name = ?");

        ResultSet rs = null;
        try {
            PreparedStatement ps = SqlUtils.getPreparedStatement(
                    platform.getDataSource(), sql.toString());
            ps.setInt(1, commentId);
            ps.setString(2, databaseName);
            rs = ps.executeQuery();

            ArrayList<String> ids = new ArrayList<String>();
            while (rs.next()) {
                String sourceId = rs.getString("source_id");
                ids.add(sourceId);
            }
            if (ids.size() > 0) {
                comment.addReference(ids.toArray(new String[ids.size()]),
                        databaseName);
            }
        } finally {
            SqlUtils.closeResultSet(rs);
        }
    }

    private void loadTargetCategoryNames(int commentId, Comment comment)
            throws SQLException {
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT b.category ");
        sql.append(" FROM ");
        sql.append(config.getCommentSchema() + "commentTargetCategory a, ");
        sql.append(config.getCommentSchema() + "targetCategory b ");
        sql.append("WHERE a.target_category_id = b.target_category_id AND a.comment_id = ?");

        ResultSet rs = null;
        try {
            PreparedStatement ps = SqlUtils.getPreparedStatement(
                    platform.getDataSource(), sql.toString());
            ps.setInt(1, commentId);
            rs = ps.executeQuery();

            ArrayList<String> ids = new ArrayList<String>();
            while (rs.next()) {
                String category = rs.getString("category");
                ids.add(category);
            }
            if (ids.size() > 0) {
                comment.addTargetCategoryNames(ids.toArray(new String[ids.size()]));
            }
        } finally {
            SqlUtils.closeResultSet(rs);
        }
    }

    private void loadMutantMarkerNames(int commentId, Comment comment)
            throws SQLException {
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT b.mutant_marker ");
        sql.append(" FROM ");
        sql.append(config.getCommentSchema() + "PhenotypeMutantMarker a, ");
        sql.append(config.getCommentSchema() + "MutantMarker b ");
        sql.append("WHERE a.mutant_marker_id = b.mutant_marker_id AND a.comment_id = ?");

        ResultSet rs = null;
        try {
            PreparedStatement ps = SqlUtils.getPreparedStatement(
                    platform.getDataSource(), sql.toString());
            ps.setInt(1, commentId);
            rs = ps.executeQuery();

            ArrayList<String> ids = new ArrayList<String>();
            while (rs.next()) {
                String mutant_marker = rs.getString("mutant_marker");
                ids.add(mutant_marker);
            }
            if (ids.size() > 0) {
                comment.addMutantMarkerNames(ids.toArray(new String[ids.size()]));
            }
        } finally {
            SqlUtils.closeResultSet(rs);
        }
    }

    private void loadMutantReporterNames(int commentId, Comment comment)
            throws SQLException {
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT b.mutant_reporter ");
        sql.append(" FROM ");
        sql.append(config.getCommentSchema() + "PhenotypeMutantReporter a, ");
        sql.append(config.getCommentSchema() + "MutantReporter b ");
        sql.append("WHERE a.mutant_reporter_id = b.mutant_reporter_id AND a.comment_id = ?");

        ResultSet rs = null;
        try {
            PreparedStatement ps = SqlUtils.getPreparedStatement(
                    platform.getDataSource(), sql.toString());
            ps.setInt(1, commentId);
            rs = ps.executeQuery();

            ArrayList<String> ids = new ArrayList<String>();
            while (rs.next()) {
                String mutant_reporter = rs.getString("mutant_reporter");
                ids.add(mutant_reporter);
            }
            if (ids.size() > 0) {
                comment.setMutantReporterNames(ids.toArray(new String[ids.size()]));
            }
        } finally {
            SqlUtils.closeResultSet(rs);
        }
    }

    private void loadPhenotype(int commentId, Comment comment)
            throws SQLException {
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
        sql.append(config.getCommentSchema() + "Phenotype a, ");
        sql.append(config.getCommentSchema() + "MutantType b, ");
        sql.append(config.getCommentSchema() + "MutantMethod c, ");
        sql.append(config.getCommentSchema() + "MutantStatus d, ");
        sql.append(config.getCommentSchema() + "MutantExpression e, ");
        sql.append("(SELECT pmc.comment_id, ");
        sql.append("apidb.tab_to_string(cast(collect(mc.mutant_category) as apidb.varchartab),'; ') as mutant_category_name ");
        sql.append(" FROM ");
        sql.append(config.getCommentSchema() + "MutantCategory mc, ");
        sql.append(config.getCommentSchema() + "PhenotypeMutantCategory pmc ");
        sql.append("WHERE mc.mutant_category_id = pmc.mutant_category_id ");
        sql.append("GROUP BY pmc.comment_id) f, ");
        sql.append(config.getCommentSchema() + "PhenotypeLoc g ");
        sql.append("WHERE a.mutant_type_id = b.mutant_type_id ");
        sql.append("AND a.mutant_method_id = c.mutant_method_id(+) ");
        sql.append("AND a.mutant_status_id = d.mutant_status_id(+) ");
        sql.append("AND a.mutant_expression_id = e.mutant_expression_id(+) ");
        sql.append("AND a.phenotype_loc_id = g.phenotype_loc_id(+) ");
        sql.append("AND a.comment_id = ?");

        ResultSet rs = null;
        try {
            PreparedStatement ps = SqlUtils.getPreparedStatement(
                    platform.getDataSource(), sql.toString());
            ps.setInt(1, commentId);
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

        } finally {
            SqlUtils.closeResultSet(rs);
        }
    }

    private void loadFiles(int commentId, Comment comment) throws SQLException {
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT file_id, name, notes ");
        sql.append(" FROM ");
        sql.append(config.getCommentSchema() + "commentFile ");
        sql.append("WHERE comment_id = ?");

        ResultSet rs = null;
        try {
            PreparedStatement ps = SqlUtils.getPreparedStatement(
                    platform.getDataSource(), sql.toString());
            ps.setInt(1, commentId);
            rs = ps.executeQuery();

            ArrayList<String> ids = new ArrayList<String>();
            while (rs.next()) {
                int file_id = rs.getInt("file_id");
                String name = rs.getString("name");
                String notes = rs.getString("notes");
                ids.add(file_id + "|" + name + "|" + notes);
            }
            if (ids.size() > 0) {
                comment.addFiles(ids.toArray(new String[ids.size()]));
            }
        } finally {
            SqlUtils.closeResultSet(rs);
        }
    }

    private void loadStableIds(int commentId, Comment comment)
            throws SQLException {
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT stable_id ");
        sql.append(" FROM ");
        sql.append(config.getCommentSchema() + "CommentStableId ");
        sql.append("WHERE comment_id = ?");

        ResultSet rs = null;
        try {
            PreparedStatement ps = SqlUtils.getPreparedStatement(
                    platform.getDataSource(), sql.toString());
            ps.setInt(1, commentId);
            rs = ps.executeQuery();

            ArrayList<String> ids = new ArrayList<String>();
            while (rs.next()) {
                String stable_id = rs.getString("stable_id");
                ids.add(stable_id);
            }
            if (ids.size() > 0) {
                comment.addAssociatedStableIds(ids.toArray(new String[ids.size()]));
            }
        } finally {
            SqlUtils.closeResultSet(rs);
        }
    }

    private void loadLocations(int commentId, Comment comment)
            throws SQLException {
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT location_start, location_end, is_reverse, ");
        sql.append("coordinate_type FROM ");
        sql.append(config.getCommentSchema() + "locations ");
        sql.append("WHERE comment_id = ?");

        ResultSet rs = null;
        try {
            PreparedStatement ps = SqlUtils.getPreparedStatement(
                    platform.getDataSource(), sql.toString());
            ps.setInt(1, commentId);
            rs = ps.executeQuery();

            while (rs.next()) {
                long start = rs.getLong("location_start");
                long end = rs.getLong("location_end");
                boolean reversed = rs.getBoolean("is_reverse");
                String coordinateType = rs.getString("coordinate_type");
                comment.addLocation(reversed, start, end, coordinateType);
            }
        } finally {
            SqlUtils.closeResultSet(rs);
        }
    }

    private void loadExternalDbs(int commentId, Comment comment)
            throws SQLException {
        String commentSchema = config.getCommentSchema();

        StringBuffer sql = new StringBuffer();
        sql.append("SELECT ed.external_database_name, ed.external_database_version ");
        sql.append("FROM " + commentSchema + "external_databases ed, ");
        sql.append(commentSchema + "comment_external_database ced ");
        sql.append("WHERE ced.comment_id = ?");
        sql.append(" AND ced.external_database_id = ed.external_database_id");

        ResultSet rs = null;
        try {
            PreparedStatement ps = SqlUtils.getPreparedStatement(
                    platform.getDataSource(), sql.toString());
            ps.setInt(1, commentId);
            rs = ps.executeQuery();

            while (rs.next()) {
                String externalDbName = rs.getString("external_database_name");
                String externalDbVersion = rs.getString("external_database_version");
                comment.addExternalDatabase(externalDbName, externalDbVersion);
            }
        } finally {
            SqlUtils.closeResultSet(rs);
        }
    }

    public Comment[] queryComments(String email, String projectName,
            String stableId, String conceptual, String reviewStatus,
            String keyword, String commentTargetId) throws WdkModelException {
        DataSource dataSource = platform.getDataSource();

        StringBuffer where = new StringBuffer();
        if (email != null) {
            email = email.replace('*', '%');
            email = email.replaceAll("'", "");
            where.append(" lower(c.email) like lower('" + email + "')");
        }
        if (projectName != null) {
            projectName = projectName.replace('*', '%');
            projectName = projectName.replaceAll("'", "");
            if (where.length() > 0) where.append(" AND");
            where.append(" c.project_name like '" + projectName + "'");
        }
        if (stableId != null) {
            stableId = stableId.replace('*', '%');
            stableId = stableId.replaceAll("'", "");
            if (where.length() > 0) where.append(" AND ");
            where.append(" (c.stable_id like '" + stableId + "'");
            where.append(" OR d.stable_id like '" + stableId + "') ");
        }
        if (conceptual != null) {
            boolean concpt = Boolean.parseBoolean(conceptual);
            if (where.length() > 0) where.append(" AND ");
            where.append(" c.conceptual like " + concpt);
        }
        if (reviewStatus != null) {
            reviewStatus = stableId.replace('*', '%');
            reviewStatus = stableId.replaceAll("'", "");
            if (where.length() > 0) where.append(" AND ");
            where.append(" c.review_status like '" + reviewStatus + "'");
        }
        if (keyword != null) {
            keyword = stableId.replace('*', '%');
            keyword = stableId.replaceAll("'", "");
            if (where.length() > 0) where.append(" AND ");
            where.append(" (c.headline like '" + keyword);
            where.append("' OR c.content like '" + keyword + "')");
        }
        if (commentTargetId != null) {
            if (where.length() > 0) where.append(" AND ");
            where.append(" c.comment_target_id = '" + commentTargetId + "'");
        }

        StringBuffer sql = new StringBuffer();
        sql.append("SELECT distinct * FROM ( ");
        sql.append("SELECT c.comment_id FROM ");
        sql.append(config.getCommentSchema() + "comments c, ");
        sql.append(config.getCommentSchema() + "commentStableId d, ");
        sql.append(config.getUserLoginSchema() + "users"
                + config.getUserLoginDbLink() + " u ");
        sql.append("WHERE c.user_id = u.user_id ");
        sql.append("AND c.is_visible = 1 ");
        sql.append("AND c.comment_id = d.comment_id(+) ");
        if (where.length() > 0) sql.append(" AND " + where.toString());
        sql.append(" ) ORDER BY comment_id DESC");

        List<Comment> comments = new ArrayList<Comment>();
        ResultSet rs = null;
        try {
            rs = SqlUtils.executeQuery(dataSource, sql.toString());
            while (rs.next()) {
                int commentId = rs.getInt("comment_id");
                Comment comment = getComment(commentId);
                comments.add(comment);
            }
        } catch (SQLException ex) {
            throw new WdkModelException(ex);
        } finally {
            SqlUtils.closeResultSet(rs);

            // print connection status
            printStatus();
        }
        Comment[] array = new Comment[comments.size()];
        comments.toArray(array);

        return array;
    }

    public void deleteComment(String email, String commentId) throws WdkModelException {
        String commentSchema = config.getCommentSchema();
        DataSource dataSource = platform.getDataSource();

        ResultSet rs = null;
        try {
            // update comments table set is_visible = 0
            String sql = "UPDATE " + commentSchema + "comments "
                    + "SET is_visible = 0 "
                    + "WHERE comment_id = '" + commentId + "'"
                    + "  AND email = '" + email + "'";
            SqlUtils.executeUpdate(dataSource, sql);

        } catch (SQLException ex) {
            throw new WdkModelException(ex);
        } finally {
            SqlUtils.closeResultSet(rs);

            // print connection status
            printStatus();
        }
    }

    public ArrayList getMultiBoxData(String nameCol, 
                                     String valueCol, 
                                     String table, 
                                     String condition) {
        
        ArrayList list = new ArrayList();
        ResultSet rs = null;

        StringBuffer sql = new StringBuffer();
        sql.append("SELECT " + nameCol + "," +  valueCol);
        sql.append(" FROM  " + config.getCommentSchema() + table);
        if (condition != null) {
          sql.append(" WHERE " + condition);
        }

        MultiBox multiBox = null;

        try {
            PreparedStatement ps = SqlUtils.getPreparedStatement(
                    platform.getDataSource(), sql.toString());
            rs = ps.executeQuery();

            while (rs.next()) {
                String name = rs.getString(nameCol);
                int value = rs.getInt(valueCol);
                multiBox = new MultiBox(name, value+"");
                list.add(multiBox);

            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public CommentConfig getCommentConfig() {
        return config;
    }

    private void printStatus() {
        int active = platform.getActiveCount();
        int idle = platform.getIdleCount();
        logger.info("Comment connections: active=" + active + ", idle=" + idle);
    }
}
