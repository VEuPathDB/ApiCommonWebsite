package org.apidb.apicommon.taglib;

/**
Parse a specified java config file to a XML Document and export it to the JSP page scope.
Feb 6, 2008, carypen@uga.edu,mheiges@uga.edu
**/

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import javax.servlet.ServletContext;

public class ConfigParser extends SimpleTagSupport {

    protected String var;
    protected String configfile;
    protected int varScope;
    
    public ConfigParser() {
        varScope = PageContext.PAGE_SCOPE;
    }
    
    public void setVar(String name) throws JspException {
        var = name;
    }
    public void setConfigfile(String file) throws JspException {
        configfile = file;
    }

    @SuppressWarnings( "unchecked" )
    public void doTag() throws JspException { 

        if (var == null || configfile == null) return;

        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        Document config = null;
        PageContext pageContext = (PageContext) getJspContext();
        ServletContext app = pageContext.getServletContext();

        InputStream is = app.getResourceAsStream(configfile);
;
        if (is == null)
            throw new JspException(
                "Failed parsing configfile '" + configfile + "'." +
                "\nCheck that the file exists and is readable: " + app.getRealPath(configfile) );
            
        try {	
	    DocumentBuilder dB = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            config = dB.parse(is);
        } catch (IOException e) {
            throw new JspException(e);
        } catch (Exception e) {
            throw new JspException(e);
        }
	NodeList site_nodes = config.getElementsByTagName("model");
        export(site_nodes);
    } 

    public void export(NodeList nodeList){
	LinkedHashMap<String,String> map = new LinkedHashMap<String,String>(); 
	JspContext jspContext = getJspContext();
	String projectId,url;
	for(int i=0;i<nodeList.getLength();i++){
	    Node node = nodeList.item(i);
	    NamedNodeMap namedNodeMap = node.getAttributes();
	    projectId = namedNodeMap.getNamedItem("projectId").getNodeValue();
	    url = namedNodeMap.getNamedItem("url").getNodeValue();
	    map.put(projectId,url);
	}
	jspContext.setAttribute(var,(Object)map,varScope);
    }
}
