/**
 * 
 */
package org.apidb.apicommon.model.test;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.apidb.apicommon.model.Comment;
import org.apidb.apicommon.model.CommentFactory;
import org.gusdb.wdk.model.WdkModelException;

/**
 * @author xingao
 * 
 */
public class CommentTest {

    private static String[] keys = { "config", "email", "headline", "content",
            "projectid", "stableid", "commenttarget", "conceptual",
            "locations", "reversed" };
    private static boolean[] required = { true, true, false, true, true, true,
            true, false, false, false };

    private static Map<String, Boolean> knownParams = new HashMap<String, Boolean>();

    static {
        for (int i = 0; i < keys.length; i++) {
            knownParams.put(keys[i], required[i]);
        }
    }

    /**
     * @param args
     * @throws WdkModelException
     * @throws MalformedURLException
     */
    public static void main(String[] args) throws MalformedURLException,
            WdkModelException {
        // parse parameters
        Map<String, String> params = parseParameters(args);
        String configXml = params.get("config");
        String email = params.get("email");
        String headline = params.get("headline");
        String content = params.get("content");
        String projectId = params.get("projectid");
        String stableId = params.get("stableid");
        String commentTarget = params.get("commenttarget");
        String conceptual = params.get("conceptual");
        String locations = params.get("locations");
        String reversed = params.get("reversed");

        // initialize comment factory
        File configDir = new File(System.getProperties().getProperty(
                "configDir"));
        File configFile = new File(configDir, configXml);
        CommentFactory.initialize(configFile.toURL());
        CommentFactory factory = CommentFactory.getInstance();

        // create comment
        Comment comment = new Comment(email);
        if (headline != null) comment.setHeadline(headline);
        if (content != null) comment.setContent(content);
        if (projectId != null) comment.setProjectId(projectId);
        if (stableId != null) comment.setStableId(stableId);
        if (commentTarget != null) comment.setCommentTarget(commentTarget);
        if (conceptual != null)
            comment.setConceptual(Boolean.parseBoolean(conceptual));
        if (locations != null) {
            boolean rev = (reversed != null && reversed.equalsIgnoreCase("true")) ? true
                    : false;
            comment.setLocations(rev, locations);
        }

        // add comment into database
        factory.addComment(comment);

        // print out the comment
        System.out.println("The comment is:\n================================");
        System.out.println(comment.toString());
    }

    private static Map<String, String> parseParameters(String[] args) {

        // get the parameters
        Map<String, String> params = new HashMap<String, String>();

        // there must be even number of args, the key value pairs
        if (args.length % 2 != 0) {
            printUsage("Unmatched key & values of the arguments");
            System.exit(-1);
        }

        for (int i = 0; i < args.length; i += 2) {
            // the key part should start with a '-'
            String key = args[i].trim().toLowerCase();
            String value = args[i + 1].trim();

            // check if the key starts with '-'
            if (key.charAt(0) != '-') {
                printUsage("Invalid key format: " + key);
                System.exit(-1);
            }
            // check if the key is known
            key = key.substring(1).trim();
            if (!knownParams.containsKey(key)) {
                printUsage("Undefined argument: " + key);
                System.exit(-1);
            }
            params.put(key, value);
        }
        // check if all required params are present
        for (String key : knownParams.keySet()) {
            if (knownParams.get(key) && !params.containsKey(key)) {
                printUsage("The required argument is missing: " + key);
                System.exit(-1);
            }
        }
        return params;
    }

    private static void printUsage(String message) {
        System.err.println("Error occurred: " + message);
        System.err.println("Usage: commentTest -config <config_file>\n"
                + "\t-email <user_email>\n"
                + "\t[-headline <headline_text, quoted>]\n"
                + "\t-content <content_text, quoted>\n"
                + "\t-projectId <site_id>\n" + "\t-stableId <source_id>\n"
                + "\t-commentTarget <gene, protein, or genome>\n"
                + "\t[-conceptual <true/false>]"
                + "\t[-locations <location pairs, eg. 12-17,245-567 >]"
                + "\t[-reversed <true/false>]");
    }
}
