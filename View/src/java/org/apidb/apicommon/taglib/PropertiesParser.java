package org.apidb.apicommon.taglib;

/**
Parse a specified java property file in to a LinkedHashMap and export 
the collection in to the JSP page scope.
27 Jan 2008, mheiges@uga.edu
**/

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;

import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import javax.servlet.ServletContext;

public class PropertiesParser extends SimpleTagSupport {

    protected String var;
    protected String propFile;
    protected int varScope;
    
    public PropertiesParser() {
        varScope = PageContext.PAGE_SCOPE;
    }
    
    public void setVar(String name) throws JspException {
        var = name;
    }
    public void setPropfile(String file) throws JspException {
        propFile = file;
    }

    @SuppressWarnings( "unchecked" )
    public void doTag() throws JspException { 

        if (var == null || propFile == null) return;

        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        Properties props = new Properties();
        PageContext pageContext = (PageContext) getJspContext();
        ServletContext app = pageContext.getServletContext();

        InputStream is = app.getResourceAsStream(propFile);
        
        if (is == null)
            throw new JspException(
                "Failed parsing propFile '" + propFile + "'." +
                "\nCheck that the file exists and is readable: " + app.getRealPath(propFile) );
            
        try {
            props.load(is);
        } catch (IOException e) {
            throw new JspException(e);
        }
        
        Enumeration<String> enumeration = (Enumeration<String>) props.propertyNames();
        List<String> keyList = Collections.list(enumeration);
        Collections.sort(keyList);

        for (String key: keyList) {
            String value = props.getProperty(key);
            map.put(key, value);
        }

        export(map);
    } 

    protected boolean export(Object value) {
        JspContext jspContext = getJspContext();
        if (value != null)
            jspContext.setAttribute(var, value, varScope);
        else
            jspContext.removeAttribute(var, varScope);
        return true;
    }
    

}
