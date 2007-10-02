/**
 * 
 */
package org.apidb.apicomment.model;

import java.io.File;

import org.apidb.apicommon.model.Comment;
import org.apidb.apicommon.model.CommentFactory;
import org.apidb.apicommon.model.ExternalDatabase;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModelException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author xingao
 * 
 */
public class CommentFactoryGetTest {

    private static final String SAMPLE_EMAIL = "sample@email";
    private static final String SAMPLE_STABLE_ID = "PF11_0344";
    private static final String SAMPLE_PROJECT_VERSION = "1.1";
    private static final String SAMPLE_COMMENT_TARGET = "gene";
    private static final String SAMPLE_KEYWORD = "test";
    private static final String SAMPLE_EXTERNAL_DATABASE = "PDB";
    private static final String SAMPLE_EXTERNAL_DATABASE_VERSION = "2.0";

    private static String projectId;
    private static CommentFactory factory;

    private int commentId;

    @BeforeClass
    public static void loadFactory() throws WdkModelException {
        // get the projectId
        String gusHome = System.getProperty(Utilities.SYSTEM_PROPERTY_GUS_HOME);
        projectId = System.getProperty(Utilities.SYSTEM_PROPERTY_PROJECT_ID);

        if (gusHome == null || projectId == null)
            throw new WdkModelException("The required system property "
                    + Utilities.SYSTEM_PROPERTY_GUS_HOME + " or "
                    + Utilities.SYSTEM_PROPERTY_PROJECT_ID + " is missing.");

        // initialize comment factory
        File configFile = new File(gusHome, "/config/" + projectId
                + "/comment-config.xml");
        CommentFactory.initialize(configFile);
        factory = CommentFactory.getInstance();
    }

    @Before
    public void addComment() throws WdkModelException {
        Comment comment = new Comment(SAMPLE_EMAIL);
        comment.setStableId(SAMPLE_STABLE_ID);
        comment.setCommentTarget(SAMPLE_COMMENT_TARGET);
        comment.setProjectName(projectId);
        comment.setProjectVersion(SAMPLE_PROJECT_VERSION);
        comment.setHeadline("A " + SAMPLE_KEYWORD + " comment");
        comment.setContent("The content of a sample content");
        comment.addExternalDatabase(SAMPLE_EXTERNAL_DATABASE,
                SAMPLE_EXTERNAL_DATABASE_VERSION);

        factory.addComment(comment);

        // get the comment id
        commentId = comment.getCommentId();
    }

    @After
    public void removeComment() throws WdkModelException {
        factory.deleteComment(commentId);
    }

    @Test
    public void testGetCommentById() throws WdkModelException {
        Comment comment = factory.getComment(commentId);
        assertEquals("comment id", commentId, comment.getCommentId());
        assertEquals("project id", projectId, comment.getProjectName());
        assertEquals("project version", SAMPLE_PROJECT_VERSION, comment
                .getProjectVersion());
        assertEquals("stable id", SAMPLE_STABLE_ID, comment.getStableId());
        assertEquals("comment target", SAMPLE_COMMENT_TARGET, comment
                .getCommentTarget());

        // check the external database info
        ExternalDatabase[] exdbs = comment.getExternalDbs();
        assertEquals("external database", SAMPLE_EXTERNAL_DATABASE, exdbs[0]
                .getExternalDbName());
        assertEquals("external database version",
                SAMPLE_EXTERNAL_DATABASE_VERSION, exdbs[0]
                        .getExternalDbVersion());
    }
}
