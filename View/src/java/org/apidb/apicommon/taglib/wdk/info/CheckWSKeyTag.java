package org.apidb.apicommon.taglib.wdk.info;

/**
Compare value from 'key' param in a request with value from secret key
file on disk. If matching, do nothing (allowing JSP to continue processing).
If not matching, forward to error page.
Intended use for restricting access to siteinfo related pages.
12 April 2011, mheiges@uga.edu
**/


import java.io.IOException;
import java.io.FileInputStream;
import java.util.Scanner;
import java.util.regex.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletException;
import org.apidb.apicommon.taglib.wdk.WdkTagBase;

import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import javax.servlet.ServletContext;

public class CheckWSKeyTag extends WdkTagBase {

    protected String keyFile;
    protected String regexFilter = "^[A-Za-z0-9]+";
    private HttpServletRequest request;
    private String invalidKeyPage = "/admin/invalidkey.jsp";
    
    public CheckWSKeyTag() {
        super();
    }
    
    public void setKeyFile(String keyFile) throws JspException {
        this.keyFile = keyFile;
    }
    public void setRegexFilter(String filter) throws JspException {
        this.regexFilter = filter;
    }

    @SuppressWarnings( "unchecked" )
    public void doTag() throws JspException { 

         request = (HttpServletRequest) this.getRequest();

       if (keyFile == null) return;

        PageContext pageContext = (PageContext) getJspContext();
        ServletContext app = pageContext.getServletContext();

        try { 
            String secretkey = getSecretKey();
            String paramkey = request.getParameter("key");

            if ( ! secretkey.equals(paramkey))
                request.getRequestDispatcher(invalidKeyPage).
                        forward(request, this.getResponse());
        } catch (IOException e) {
            throw new JspException(e);
        } catch (ServletException se) {
            throw new JspException(se);
        }

    } 
    private String getSecretKey() throws IOException {
        StringBuilder text = new StringBuilder();
        Pattern p = Pattern.compile(regexFilter);

        Scanner scanner = new Scanner(new FileInputStream(keyFile), "UTF-8");
        try {
          while (scanner.hasNextLine()){
            String line = scanner.nextLine();
            Matcher m = p.matcher(line);
            if (m.find()) {
               text.append(line);
            }
          }
        }
        finally{
          scanner.close();
        }
        return text.toString().trim();
    }
}