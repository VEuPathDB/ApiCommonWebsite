package org.apidb.apicommon.controller;

/**
 * Utility class to sanitize HTML and convert the limited BBCode to HTML. 
 * 
 * Sanitizing HTML is limited to converting all the '<' and '>' symbols to the corresponding HTML
 * entities, and converting new line characters to '<br/>' tags. A very limited subset of BBCode is 
 * supported, only those tags that deal with formatting: i, b, u, ul, li, sup, sub. Additionally, strings
 * looking like URLs are convereted to hyperlinks.
 * 
 * To keep it simple, the BBCode->HTML conversion relies on the calling functions enclosing the resulting 
 * string in a HTML table, so that this class doesn't need to take care of closing any HTML (generated 
 * from BBCode) tags. Though the API allows enabling additional BBCode->HTML conversions, it is important
 * to make sure none of the HTML table related tags are enabled. In other words, only those BBCode tags 
 * can be enabled which are implicitly closed when a HTML table tag (tr, td, table) is closed. Another criteria
 * was that, the allowed BBCode tags should be easy enough to be captured in simple regular expressions, i.e., 
 * tags with attributes are not supported.
 * 
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BBCode {

    private static final String[] initBBCodes = { "b", "i", "u", "ul", "ol",
            "li", "sup", "sub" };

    private Hashtable<String, String> beginBBCode, endBBCode;
    // URL can be of the form: protocol://foo.tld or protocol://www.foo.tld or
    // just www.foo.tld
    // No further restrictions are placed on either the protocol or the tld
    // This pattern does miss some URLs such as www2.foo.tld, but writing it as
    // http://www2.foo.tld
    // makes sure that it gets converted to a clickable URL.
    private static Pattern urlPattern1 = Pattern.compile("(([a-zA-Z]+://(www\\.)?)|(www\\.))([a-zA-Z0-9]+\\.)+[a-zA-Z]{2,3}");
    private static BBCode bbcodeInstance = null;

    public BBCode() {
        beginBBCode = new Hashtable<String, String>();
        endBBCode = new Hashtable<String, String>();

        for (int i = 0; i < initBBCodes.length; i++)
            addAllowedBBCode(initBBCodes[i]);
    }

    public static BBCode getInstance() {
        if (bbcodeInstance == null) {
            bbcodeInstance = new BBCode();
        }

        return bbcodeInstance;
    }

    public void addAllowedBBCode(String bbcode) {
        beginBBCode.put("\\[" + bbcode + "\\]", "<" + bbcode + ">");
        endBBCode.put("\\[/" + bbcode + "\\]", "</" + bbcode + ">");
    }

    public void removeAllowedBBCode(String bbcode) {
        beginBBCode.remove("\\[" + bbcode + "\\]");
        endBBCode.remove("\\[/" + bbcode + "\\]");
    }

    public String sanitizeHtml(String str) {
        str = str.replaceAll("&", "&amp;");
        str = str.replaceAll("<", "&lt;");
        str = str.replaceAll(">", "&gt;");
        str = str.replaceAll("\r?\n", "<br/>");

        return str;
    }

    public String convertBBCodeToHtml(String str) {
        // convert all begin BBCode tags
        str = sanitizeHtml(str);
        for (String key : beginBBCode.keySet()) {
            String replacement = beginBBCode.get(key);
            str = str.replaceAll(key, replacement);
        }

        // convert all end BBCode tags
        for (String key : endBBCode.keySet()) {
            String replacement = endBBCode.get(key);
            str = str.replaceAll(key, replacement);
        }

        // convert all URL like occurences to hyperlinks
        Matcher matcher = urlPattern1.matcher(str);

        for (int start = 0; matcher.find(start);) {
            String match = matcher.group();
            start = matcher.end();

            String url = match;
            if (url.indexOf("://") < 0) url = "http://" + match;

            String replacement = "<a href=\"" + url + "\">" + match + "</a>";
            System.out.println(match + " => " + replacement);
            str = str.replaceFirst(match, replacement);

        }

        return str;
    }

    public static void main(String[] args) {
        File commentFile = new File("/home/praveenc/sandbox/comment.txt");
        String comment = "";

        try {
            BufferedReader br = new BufferedReader(new FileReader(commentFile));
            String line;
            while ((line = br.readLine()) != null)
                comment += line + "\n";
            br.close();

            String header = "<html><title>PlasmoDB comments page</title>\n"
                    + "<body>" + "<h3>Original Comment</h3><hr/>" + "<pre>"
                    + comment + "</pre><hr/><br/><br/>"
                    + "<h3>User comments</h3><hr/><table>";
            String footer = "</table><hr/><h3>End of user comments</h3>";

            FileWriter fw = new FileWriter(
                    "/home/praveenc/sandbox/comment.html");
            fw.write(header);
            fw.write("<tr><td>" + new BBCode().convertBBCodeToHtml(comment)
                    + "</td></tr>");
            fw.write(footer);
            fw.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
