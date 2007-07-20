/**
 * 
 */
package org.apidb.apicommon.model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.gusdb.wdk.model.RDBMSPlatformI;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.implementation.SqlUtils;
import org.xml.sax.SAXException;

/**
 * @author xingao
 * 
 */
public class CommentFactory {

    private static CommentFactory factory;
    
    private Logger logger = Logger.getLogger (CommentFactory.class); 
    private RDBMSPlatformI platform;
    private CommentConfig config;
    
    public static void initialize(URL commentConfigXmlUrl)
            throws WdkModelException {
        try {
            // parse and load the configuration
            CommentConfig config = CommentConfigParser.parseXmlFile(commentConfigXmlUrl);

            // create a platform object
            RDBMSPlatformI platform = (RDBMSPlatformI) Class.forName(
                    config.getPlatformClass()).newInstance();
            platform.init(config.getConnectionUrl(), config.getLogin(),
                    config.getPassword(), config.getMinIdle(),
                    config.getMaxIdle(), config.getMaxWait(),
                    config.getMaxActive(), config.getInitialSize(),
                    commentConfigXmlUrl.getFile());

            // create a factory instance
            factory = new CommentFactory(platform, config);
            
        } catch (InstantiationException ex) {
            throw new WdkModelException(ex);
        } catch (IllegalAccessException ex) {
            throw new WdkModelException(ex);
        } catch (ClassNotFoundException ex) {
            throw new WdkModelException(ex);
        } catch (SAXException ex) {
            throw new WdkModelException(ex);
        } catch (IOException ex) {
            throw new WdkModelException(ex);
        }
    }

    public static CommentFactory getInstance() throws WdkModelException {
        if (factory == null)
            throw new WdkModelException(
                    "Please initialize the factory properly.");
        return factory;
    }

    private CommentFactory(RDBMSPlatformI platform, CommentConfig config) {
        this.platform = platform;
        this.config = config;
    }

    public CommentTarget getCommentTarget(String internalValue)
            throws WdkModelException {
        String schema = config.getCommentSchema();

        // load the comment target definition of the given internal value

        ResultSet rs = null;
        CommentTarget target = null;
        PreparedStatement ps = null;
        String query = null;
        try {
            query = "SELECT * FROM " + schema
                    + ".comment_target WHERE comment_target_id=?";
            ps = SqlUtils.getPreparedStatement(platform.getDataSource(), query);
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
            try {
                SqlUtils.closeResultSet(rs);
            } catch (SQLException e) {}
        }
        return target;
    }

    public void addComment(Comment comment) throws WdkModelException {
        String schema = config.getCommentSchema();

        PreparedStatement ps = null;
        // get a new comment id
        try {
            // DataSource dataSource = platform.getDataSource();
            String commentId = platform.getNextId(schema, "comments");

            ps = SqlUtils.getPreparedStatement(
                    platform.getDataSource(),
                    "INSERT INTO "
                            + schema
                            + ".comments (comment_id, email, "
                            + "comment_date, comment_target_id, stable_id, conceptual, "
                            + "project_name, project_version, headline, content, "
                            + "location_string, review_status_id) "
                            + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");

            long currentMillis = System.currentTimeMillis();

            ps.setInt(1, Integer.parseInt(commentId));
            ps.setString(2, comment.getEmail());
            ps.setDate(3, new java.sql.Date(currentMillis));
            ps.setString(4, comment.getCommentTarget());
            ps.setString(5, comment.getStableId());
            ps.setBoolean(6, comment.isConceptual());
            ps.setString(7, comment.getProjectName());
            ps.setString(8, comment.getProjectVersion());
            ps.setString(9, comment.getHeadline());
            ps.setString(10, comment.getContent());
            ps.setString(11, comment.getLocationString());
            String rs = (comment.getReviewStatus() != null && comment.getReviewStatus().length() > 0) 
            			? comment.getReviewStatus()
            			: Comment.COMMENT_REVIEW_STATUS_UNKNOWN;
            ps.setString(12, rs);

            ps.execute();

            // update the fields of comment
            comment.setCommentDate(new java.util.Date(currentMillis));
            comment.setCommentId(commentId);

            // then add the location information
            saveLocations(commentId, comment);

            // then add the eternal database information
            
            saveExternalDbs(commentId, comment);
           
            File cFile = new File (config.getCommentTextFileDir() + System.getProperty("file.separator")
            						+ comment.getProjectName() + "_comments.txt");	
            extractCommentsTextSearchFile (cFile);
            
        } catch (SQLException ex) {
            throw new WdkModelException(ex);
        }
    }

    private void saveLocations(String commentId, Comment comment)
            throws SQLException {
        String schema = config.getCommentSchema();

        // construct sql
        StringBuffer sql = new StringBuffer();
        sql.append("INSERT INTO " + schema + ".locations (comment_id, ");
        sql.append("location_id, location_start, location_end, is_reverse, ");
        sql.append("coordinate_type) VALUES (?, ?, ?, ?, ?, ?)");
        PreparedStatement statement = null;
        try {
            statement = SqlUtils.getPreparedStatement(platform.getDataSource(),
                    sql.toString());

            Location[] locations = comment.getLocations();
            for (Location location : locations) {
                String locationId = platform.getNextId(schema, "locations");
                statement.setInt(1, Integer.parseInt(commentId));
                statement.setInt(2, Integer.parseInt(locationId));
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

    /**
     * For the first release, this method hard-code the external database name
     * and version, instead of looking up directly in GUS. This avoids the
     * backward dblink requirement; The site should provide such information to
     * the comment factory in future releases.
     * 
     * UPDATE: Assume that the Comment parameter already has the external database info filled in.
     * Use the first extdb/versions (Is there a case at all where we need more than one ext db???)
     * 
     * @param commentId
     * @param comment
     * @throws SQLException
     */
    private void saveExternalDbs(String commentId, Comment comment)
            throws SQLException {
        String schema = config.getCommentSchema();
        // String dblink = config.getProjectDbLink();
        // String stableId = comment.getStableId();
        DataSource dataSource = platform.getDataSource();

        // get a set of matched external databases
        StringBuffer sql = new StringBuffer();
        
        PreparedStatement queryDb = null, insertDb = null, insertLink = null;
        try {
            // ps = SqlUtils.getPreparedStatement(dataSource, sql.toString());
            // ps.setString(1, stableId);
            // ps.setString (2, stableId);
            //        	
            // rs = ps.executeQuery();

            // construct prepared statements
            sql = new StringBuffer();
            sql.append("SELECT external_database_id ");
            sql.append("FROM " + schema + ".external_databases ");
            sql.append("WHERE external_database_name = ? "
                    + "AND external_database_version = ?");
            queryDb = SqlUtils.getPreparedStatement(dataSource, sql.toString());

            sql = new StringBuffer();
            sql.append("INSERT INTO " + schema + ".external_databases ");
            sql.append("(external_database_id, external_database_name, "
                    + "external_database_version) VALUES (?, ?, ?)");
            insertDb = SqlUtils.getPreparedStatement(dataSource, sql.toString());

            sql = new StringBuffer();
            sql.append("INSERT INTO " + schema + ".comment_external_database ");
            sql.append("(external_database_id, comment_id) VALUES (?, ?)");
            insertLink = SqlUtils.getPreparedStatement(dataSource,
                    sql.toString());

            // while (rs.next()) {
            // String externalDbName = rs.getString("external_database_name");
            // String externalDbVersion =
            // rs.getString("external_database_version");
           // String externalDbName = "PlasmoDB_Temp";
            //String externalDbVersion = "ver_1.0";
            
            //The external database info is provided by JSP

			String externalDbName, externalDbVersion;
            ExternalDatabase[] eds = comment.getExternalDbs();

			if (eds != null && eds.length > 0) {
            	externalDbName = eds[0].getExternalDbName();
            	externalDbVersion = eds[0].getExternalDbVersion();
			} else {
				String getExtDbSql;
				if (comment.getCommentTarget().contains("gene")) {
					getExtDbSql = "SELECT ed.name, version "
						+ "FROM sres.externaldatabase ed, sres.externaldatabaserelease edr, "
						+ "dots.genefeature gf "
						+ "WHERE ed.external_database_id = edr.external_database_id "
						+ "AND gf.external_database_release_id = edr.external_database_release_id "
						+ "AND gf.source_id=?";
				} else {
					String naSeqTable;
					if (comment.getProjectName().equalsIgnoreCase("toxodb")) {
					//toxo uses dots.virtualnasequence, others use dots.externalnasequence
						naSeqTable = "DoTS.VirtualNaSequence";
					} else {
						naSeqTable = "DoTS.ExternalNASequence";	
					}

					getExtDbSql = "SELECT ed.name, version "
						+ "FROM sres.externaldatabase ed, sres.externaldatabaserelease edr, "
						+ naSeqTable + " ns "
						+ "WHERE ed.external_database_id = edr.external_database_id "
						+ "AND ns.external_database_release_id = edr.external_database_release_id "
						+ "AND ns.source_id=?";
				}

				PreparedStatement ps = SqlUtils.getPreparedStatement (dataSource, 
															getExtDbSql.toString());
				ps.setString (1, comment.getStableId());

				ResultSet rs = ps.executeQuery();
	            if (!rs.next()) {
                	System.err.println ("Error in executing getextdbsql");
				}

				externalDbName = rs.getString("name");
				externalDbVersion = rs.getString("version");
				System.out.println ("-- " + externalDbName + ", " + externalDbVersion);
				comment.addExternalDatabase (externalDbName, externalDbVersion);
			}
            
            int externalDbId;

            // check if the external db entry exists
            queryDb.setString(1, externalDbName);
            queryDb.setString(2, externalDbVersion);
            ResultSet rsDb = queryDb.executeQuery();
            if (!rsDb.next()) { // no entry, add one
                externalDbId = Integer.parseInt(platform.getNextId(schema,
                        "external_databases"));
                insertDb.setInt(1, externalDbId);
                insertDb.setString(2, externalDbName);
                insertDb.setString(3, externalDbVersion);
                insertDb.execute();
            } else { // has entry, get the external_database_id
                externalDbId = rsDb.getInt("external_database_id");
            }
            rsDb.close();

            // add the reference link
            insertLink.setInt(1, externalDbId);
            insertLink.setInt(2, Integer.parseInt(commentId));
            insertLink.execute();
            // }
        } finally {
            // close statements and result set
            // SqlUtils.closeResultSet(rs);
            SqlUtils.closeStatement(queryDb);
            SqlUtils.closeStatement(insertDb);
            SqlUtils.closeStatement(insertLink);
        }
    }

    public Comment getComment(String commentId) throws WdkModelException {
        String schema = config.getCommentSchema();

        StringBuffer sql = new StringBuffer();
        sql.append("SELECT * FROM " + schema + ".comments ");
        sql.append("WHERE comment_id = ?");
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {

            ps = SqlUtils.getPreparedStatement(platform.getDataSource(),
                    sql.toString());
            ps.setString(1, commentId);
            rs = ps.executeQuery();

            if (!rs.next())
                throw new WdkModelException("Comment of the given id '"
                        + commentId + "' cannot be found.");

            // construct a comment object
            Comment comment = new Comment(rs.getString("email"));
            comment.setCommentId(commentId);
            comment.setCommentDate(rs.getDate("comment_date"));
            comment.setCommentTarget(rs.getString("comment_target_id"));
            comment.setConceptual(rs.getBoolean("conceptual"));
            comment.setContent(rs.getString("content"));
            comment.setHeadline(rs.getString("headline"));
            comment.setProjectName(rs.getString("project_name"));
            comment.setProjectVersion(rs.getString("project_version"));
            comment.setReviewStatus(rs.getString("review_status_id"));
            comment.setStableId(rs.getString("stable_id"));
            

            // load locations
            loadLocations(commentId, comment);

            // load external databases
            loadExternalDbs(commentId, comment);

            return comment;
        } catch (SQLException ex) {
            throw new WdkModelException(ex);
        } finally {
            try {
                SqlUtils.closeResultSet(rs);
            } catch (SQLException ex) {
                throw new WdkModelException(ex);
            }
        }
    }

    private void loadLocations(String commentId, Comment comment)
            throws SQLException {
        String schema = config.getCommentSchema();

        StringBuffer sql = new StringBuffer();
        sql.append("SELECT location_start, location_end, is_reverse, ");
        sql.append("coordinate_type FROM " + schema + ".locations ");
        sql.append("WHERE comment_id = ?");
        ResultSet rs = null;

        PreparedStatement ps = null;
        try {

            ps = SqlUtils.getPreparedStatement(platform.getDataSource(),
                    sql.toString());
            ps.setString(1, commentId);
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

    private void loadExternalDbs(String commentId, Comment comment)
            throws SQLException {
        String schema = config.getCommentSchema();

        StringBuffer sql = new StringBuffer();
        sql.append("SELECT ed.external_database_name, ed.external_database_version ");
        sql.append("FROM " + schema + ".external_databases ed, ");
        sql.append(schema + ".comment_external_database ced ");
        sql.append("WHERE ced.comment_id = ?");
        sql.append(" AND ced.external_database_id = ed.external_database_id");

        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = SqlUtils.getPreparedStatement(platform.getDataSource(),
                    sql.toString());
            ps.setString(1, commentId);
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
            String keyword) throws WdkModelException {
        String schema = config.getCommentSchema();
        DataSource dataSource = platform.getDataSource();

        StringBuffer where = new StringBuffer();
        if (email != null) {
            email = email.replace('*', '%');
            email = email.replaceAll("'", "");
            where.append(" email like '" + email + "'");
        }
        if (projectName != null) {
            projectName = projectName.replace('*', '%');
            projectName = projectName.replaceAll("'", "");
            if (where.length() > 0) where.append(" AND");
            where.append(" project_name like '" + projectName + "'");
        }
        if (stableId != null) {
            stableId = stableId.replace('*', '%');
            stableId = stableId.replaceAll("'", "");
            if (where.length() > 0) where.append(" AND ");
            where.append(" stable_id like '" + stableId + "'");
        }
        if (conceptual != null) {
            boolean concpt = Boolean.parseBoolean(conceptual);
            if (where.length() > 0) where.append(" AND ");
            where.append(" conceptual like " + concpt);
        }
        if (reviewStatus != null) {
            reviewStatus = stableId.replace('*', '%');
            reviewStatus = stableId.replaceAll("'", "");
            if (where.length() > 0) where.append(" AND ");
            where.append(" review_status like '" + reviewStatus + "'");
        }
        if (keyword != null) {
            keyword = stableId.replace('*', '%');
            keyword = stableId.replaceAll("'", "");
            if (where.length() > 0) where.append(" AND ");
            where.append(" (headline like '" + keyword);
            where.append("' OR content like '" + keyword + "')");
        }

        StringBuffer sql = new StringBuffer();
        sql.append("SELECT comment_id FROM " + schema + ".comments");
        if (where.length() > 0) sql.append(" WHERE " + where.toString());
        sql.append(" ORDER BY comment_id");

        List<Comment> comments = new ArrayList<Comment>();
        ResultSet rs = null;
        try {
            rs = SqlUtils.getResultSet(dataSource, sql.toString());
            while (rs.next()) {
                String commentId = rs.getString("comment_id");
                Comment comment = factory.getComment(commentId);
                comments.add(comment);
            }
        } catch (SQLException ex) {
            throw new WdkModelException(ex);
        } finally {
            try {
                SqlUtils.closeResultSet(rs);
            } catch (SQLException ex) {
                throw new WdkModelException(ex);
            }
        }
        Comment[] array = new Comment[comments.size()];
        comments.toArray(array);
        return array;
    }

    public void deleteComment(String commentId) throws WdkModelException {
        String schema = config.getCommentSchema();
        DataSource dataSource = platform.getDataSource();

        String where = " WHERE comment_id = " + commentId;
        String projectName = "";
        
        try {
        	String findSql = "SELECT project_name FROM " + schema + ".comments" + where;
        	ResultSet rs = SqlUtils.getResultSet(dataSource, findSql);
        	if (!rs.next()) {
        		logger.warn("DeleteComment: Did not find a comment for id " + commentId);
        		return;
        	}
        	
        	projectName = rs.getString(1);
        	
            // delete the location information
            String sql = "DELETE FROM " + schema + ".locations" + where;
            SqlUtils.execute(dataSource, sql);

            // delete associated external database info
            sql = "DELETE FROM " + schema + ".comment_external_database"
                    + where;
            SqlUtils.execute(dataSource, sql);

            // delete comment information
            sql = "DELETE FROM " + schema + ".comments" + where;
            SqlUtils.execute(dataSource, sql);
        } catch (SQLException ex) {
            throw new WdkModelException(ex);
        }
        
        try {
        	File cFile = new File (config.getCommentTextFileDir() + System.getProperty("file.separator")
        								+ projectName + "_comments.txt");
        	extractCommentsTextSearchFile (cFile);
        } catch (Exception e) {
        	logger.warn("Error in deleteComment", e);
        }
        
    }
    
    public void extractCommentsTextSearchFile (File commentsFile) {
    	
    	DataSource dataSource = platform.getDataSource();

    	logger.info("extracting flatfile " + commentsFile);

    	//TODO: Are we being oracle specific by using regexp_replace?? 
    	String getCommentsSql = "SELECT DISTINCT '', substr(tn.name, 1, instr(tn.name || '  ', ' ', 1, 2)-1), "
	    + " gf.source_id, "
	    + " regexp_replace(c.content || ' (' || u.first_name || ' ' || u.last_name || ')', '[[:space:]]', ' ') "
	    + " FROM userLogins.users" + config.getProjectDbLink() + " u, "
	    + config.getCommentSchema() + ".comments" + config.getProjectDbLink() 
	    + " c, DoTS.GeneFeature gf, DoTS.NaSequence ns, SRes.TaxonName tn "
	    + " WHERE u.email = c.email "
	    + " AND c.comment_target_id='gene' "
	    + " AND c.stable_id = gf.source_id "
	    + " AND c.review_status_id != 'rejected' "
	    + " AND gf.na_sequence_id = ns.na_sequence_id "
	    + " AND ns.taxon_id = tn.taxon_id "
	    + " AND tn.name_class = 'scientific name' "
	    + " ORDER BY substr(tn.name, 1, instr(tn.name || '  ', ' ', 1, 2)-1), gf.source_id, "
	    + " regexp_replace(c.content || ' (' || u.first_name || ' ' || u.last_name || ')', '[[:space:]]', ' ') ";
    	
	logger.info("flatfile extraction SQL: " + getCommentsSql);

    	try {
    		ResultSet rs = SqlUtils.getResultSet(dataSource, getCommentsSql);
    		FileWriter fw = new FileWriter (commentsFile);
    		while (rs.next()) {
    			//String empty = rs.getString(1);
    			String orgName = rs.getString(2);
    			String sourceId = rs.getString(3);
    			String comment = rs.getString(4);
    			fw.write("\t" + orgName + "\t" + sourceId + "\t" + comment + "\n");
    		}
    		
    		rs.close();
    		fw.close();
    	} catch (Exception e){
    		//Is there a need to throw WDK exception? 
    		logger.warn("Error in creating comments file: ", e);
    	} 
    }
}
