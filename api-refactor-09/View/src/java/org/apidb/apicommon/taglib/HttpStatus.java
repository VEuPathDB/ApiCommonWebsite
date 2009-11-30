package org.apidb.apicommon.taglib;

/**
Fetch the HTTP header and set var attribute to the status code.
Throws exceptions for connection and read timeouts - these timeouts 
can be defined with attributes.
**/

import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import javax.servlet.ServletContext;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;

import java.io.IOException;

public class HttpStatus extends SimpleTagSupport {

    protected String url;
    protected String var;
    protected int readTimeout = 0;
    protected int connectTimeout = 0;
    protected boolean followRedirect = true;
    protected int varScope;

    public HttpStatus() {
        varScope = PageContext.PAGE_SCOPE;
    }
    
    public void setUrl(String url) throws JspException {
        this.url = url;
    }
    
    public void setVar(String var) throws JspException {
        this.var = var;
    }

    public void setFollowRedirect(boolean followRedirect) throws JspException {
        this.followRedirect = followRedirect;
    }
    
    public void setReadTimeout(int readTimeout) throws JspException {
        this.readTimeout = readTimeout;
    }
    
    public void setConnectTimeout(int connectTimeout) throws JspException {
        this.connectTimeout = connectTimeout;
    }
    
    public void doTag() throws JspException { 

        try {
            URLConnection connection = new URL(url).openConnection();

            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(readTimeout);

            connection.connect();

            if ( connection instanceof HttpURLConnection) {
                HttpURLConnection httpConnection = (HttpURLConnection) connection;
                httpConnection.setInstanceFollowRedirects(followRedirect);
                int code = httpConnection.getResponseCode();
                if (var != null) getJspContext().setAttribute(var, code, varScope); 
            } else {
                throw new JspException(url + " is not a valid HTTP request");
            }

        } catch (MalformedURLException mure) {
            throw new JspException("(MalformedURLException) " + mure);
        } catch (IOException ioe) {
            throw new JspException("(IOException) " + ioe);
        }
    }
}