/**
 * 
 */
package org.apidb.apicommon.model;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.digester.Digester;
import org.gusdb.wdk.model.ModelConfig;
import org.xml.sax.SAXException;

/**
 * @author xingao
 * 
 */
public class CommentConfigParser {

    public static CommentConfig parseXmlFile(File commentConfigXmlFile)
            throws SAXException, IOException {
        Digester digester = configureDigester();
        return (CommentConfig) digester.parse(commentConfigXmlFile);
    }

    public static CommentConfig parseXmlFile(URL commentConfigXmlFile)
            throws SAXException, IOException {
        Digester digester = configureDigester();
        return (CommentConfig) digester.parse(commentConfigXmlFile.openStream());
    }

    private static Digester configureDigester() {

        Digester digester = new Digester();
        digester.setValidating(false);

        digester.addObjectCreate("commentConfig", CommentConfig.class);
        digester.addSetProperties("commentConfig");

        return digester;
    }

    public static void main(String[] args) {
        try {
            File commentConfigXmlFile = new File(args[0]);
            CommentConfig commentConfig = parseXmlFile(commentConfigXmlFile);

            System.out.println(commentConfig.toString());

        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }
}
