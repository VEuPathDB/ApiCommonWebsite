package org.apidb.apicommon.controller;

/**
 Record miscellaneous webapp startup state to file in Tomcat's tempdir.
 Enable in web.xml:
 <servlet>
 <servlet-name>onWebappStartup</servlet-name>
 <servlet-class>org.apidb.apicommon.controller.OnWebappStartup</servlet-class>
 <load-on-startup>1</load-on-startup>
 </servlet>
 **/

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;
import org.apache.xerces.dom.DocumentImpl;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class OnWebappStartup extends HttpServlet {

    /**
     * 
     */
    private static final long serialVersionUID = -8350067006943115970L;
    private Logger logger = Logger.getLogger(OnWebappStartup.class);
    private ServletContext application;
    private Document xmldoc;
    private Element root;

    public void init(ServletConfig config) throws ServletException {
        application = config.getServletContext();
        File tempdir = (File) application.getAttribute("javax.servlet.context.tempdir");
        logger.debug("Recording startup meta data to "
                + tempdir.getAbsolutePath());

        xmldoc = new DocumentImpl();
        root = xmldoc.createElement(this.getClass().getName());
        xmldoc.appendChild(root);

        recordStartTime();

        try {
            FileOutputStream fos = new FileOutputStream(tempdir + "/"
                    + ApiCommonConstants.WEBAPP_START_STATE_FILE);
            OutputFormat of = new OutputFormat("XML", "ISO-8859-1", true);
            of.setIndent(1);
            of.setIndenting(true);
            XMLSerializer serializer = new XMLSerializer(fos, of);
            serializer.asDOMSerializer();
            serializer.serialize(xmldoc.getDocumentElement());
            fos.close();
        } catch (IOException ioe) {
            logger.warn("Error recording startup data: " + ioe.getMessage());
        }

    }

    private void recordStartTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                ApiCommonConstants.ISO8601_DATE_FORMAT);
        java.util.Date date = new java.util.Date();
        String starttime = dateFormat.format(date);
        addElement("starttime", starttime);
    }

    private void addElement(String element, String node) {
        Element e = xmldoc.createElementNS(null, element);
        Node n = xmldoc.createTextNode(node);
        e.appendChild(n);
        root.appendChild(e);
    }
}