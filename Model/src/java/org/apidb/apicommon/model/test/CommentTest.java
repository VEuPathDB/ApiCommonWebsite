/**
 * 
 */
package org.apidb.apicommon.model.test;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apidb.apicommon.model.Comment;
import org.apidb.apicommon.model.CommentFactory;
import org.gusdb.wdk.model.WdkModelException;

/**
 * @author xingao
 * 
 */
public class CommentTest {

    public static final String CONFIG_XML = "comment-config.xml";

    private static String[] addKeys = { "email", "headline", "content",
            "projectName", "projectVersion", "stableId", "commentTarget",
            "conceptual", "locations", "reversed", "coordinateType" };
    private static boolean[] addRequired = { true, false, true, true, true,
            true, true, false, false, false, false };

    private static Map<String, Boolean> addParams = new LinkedHashMap<String, Boolean>();

    static {
        for (int i = 0; i < addKeys.length; i++) {
            addParams.put(addKeys[i].toLowerCase(), addRequired[i]);
        }
    }

    private static String[] listKeys = { "email", "keyword", "projectName",
            "stableId", "conceptual", "reviewStatus" };
    private static boolean[] listRequired = { false, false, false, false,
            false, false };

    private static Map<String, Boolean> listParams = new LinkedHashMap<String, Boolean>();

    static {
        for (int i = 0; i < listKeys.length; i++) {
            listParams.put(listKeys[i].toLowerCase(), listRequired[i]);
        }
    }

    private static String[] deleteKeys = { "commentId" };
    private static boolean[] deleteRequired = { true };

    private static Map<String, Boolean> deleteParams = new LinkedHashMap<String, Boolean>();

    static {
        for (int i = 0; i < deleteKeys.length; i++) {
            deleteParams.put(deleteKeys[i].toLowerCase(), deleteRequired[i]);
        }
    }

    private static CommentFactory factory;

    /**
     * @param args
     * @throws WdkModelException
     * @throws MalformedURLException
     */
    public static void main(String[] args) throws MalformedURLException,
            WdkModelException {
        if (args.length < 1) {
            printUsage("Command is missing.");
            System.exit(-1);
        }

        // determine the command
        String cmd = args[0].trim();
        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, subArgs.length);

        // initialize comment factory
        File configDir = new File(System.getProperties().getProperty(
                "configDir"));
        File configFile = new File(configDir, CONFIG_XML);
        CommentFactory.initialize(configFile.toURL());
        factory = CommentFactory.getInstance();

        if (cmd.equalsIgnoreCase("add")) {
            Map<String, String> params = prepareParameters(subArgs, addParams);
            addComment(params);
        } else if (cmd.equalsIgnoreCase("list")) {
            Map<String, String> params = prepareParameters(subArgs, listParams);
            listComments(params);
        } else if (cmd.equalsIgnoreCase("delete")) {
            Map<String, String> params = prepareParameters(subArgs,
                    deleteParams);
            deleteComment(params);
        } else {
            printUsage("Unknown command for commentTest: " + cmd);
            System.exit(-1);
        }
    }

    private static Map<String, String> prepareParameters(String[] args,
            Map<String, Boolean> knownParams) {

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

    public static void addComment(Map<String, String> params)
            throws WdkModelException {
        String email = params.get("email");
        String headline = params.get("headline");
        String content = params.get("content");
        String projectName = params.get("projectname");
        String projectVersion = params.get("projectversion");
        String stableId = params.get("stableid");
        String commentTarget = params.get("commenttarget");
        String conceptual = params.get("conceptual");
        String locations = params.get("locations");
        String reversed = params.get("reversed");
        String coordinateType = params.get("coordinatetype");

        // create comment
        Comment comment = new Comment(email);
        if (headline != null) comment.setHeadline(headline);
        if (content != null) comment.setContent(content);
        if (projectName != null) comment.setProjectName(projectName);
        if (projectVersion != null) comment.setProjectVersion(projectVersion);
        if (stableId != null) comment.setStableId(stableId);
        if (commentTarget != null) comment.setCommentTarget(commentTarget);
        if (conceptual != null)
            comment.setConceptual(Boolean.parseBoolean(conceptual));
        if (locations != null) {
            boolean rev = (reversed != null && reversed.equalsIgnoreCase("true")) ? true
                    : false;
            // default coordinate type would be gene
            if (coordinateType == null || coordinateType.length() == 0)
                coordinateType = "gene";
            comment.setLocations(rev, locations, coordinateType);
        }

        // add comment into database
        factory.addComment(comment);

        // print out the comment
        System.out.println("The comment #" +comment.getCommentId()+" is added:");
        System.out.println(comment.toString());
    }

    public static void listComments(Map<String, String> params)
            throws WdkModelException {
        String email = params.get("email");
        String keyword = params.get("keyword");
        String projectName = params.get("projectname");
        String stableId = params.get("stableId");
        String conceptual = params.get("conceptual");
        String reviewStatus = params.get("reviewstatus");

        Comment[] comments = factory.queryComments(email, projectName,
                stableId, conceptual, reviewStatus, keyword);

        // print out the comment information
        System.out.println("#ID;\tEmail;\tStable Id;\tProject Name;\tHeadline");
        for (Comment comment : comments) {
            System.out.print(comment.getCommentId() + ";\t");
            System.out.print(comment.getEmail() + ";\t");
            System.out.print(comment.getStableId() + ";\t");
            System.out.print(comment.getProjectName() + ";\t");
            System.out.println(comment.getHeadline());
        }
    }

    public static void deleteComment(Map<String, String> params)
            throws WdkModelException {
        String commentId = params.get("commentid");

        factory.deleteComment(commentId);

        System.out.println("Comment #" + commentId
                + " has been deleted from the database.");
    }

    private static void printUsage(String message) {
        System.err.println("Error occurred: " + message);
        System.err.println();
        System.err.print("Usage:\tcommentTest add");
        for (String key : addParams.keySet()) {
            boolean required = addParams.get(key);
            if (!required) System.err.print(" [-" + key + " <" + key + ">]");
            else System.err.print(" -" + key + " <" + key + ">");
        }
        System.err.println();
        System.err.println();

        System.err.print("\tcommentTest list");
        for (String key : listParams.keySet()) {
            boolean required = listParams.get(key);
            if (!required) System.err.print(" [-" + key + " <" + key + ">]");
            else System.err.print(" -" + key + " <" + key + ">");
        }
        System.err.println();
        System.err.println();

        System.err.print("\tcommentTest delete");
        for (String key : deleteParams.keySet()) {
            boolean required = deleteParams.get(key);
            if (!required) System.err.print(" [-" + key + " <" + key + ">]");
            else System.err.print(" -" + key + " <" + key + ">");
        }
        System.err.println();
    }
}
