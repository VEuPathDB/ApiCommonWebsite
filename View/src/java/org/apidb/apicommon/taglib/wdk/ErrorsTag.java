/**

Error handling tag modeled after (and meant to replace) WDK's errors.tag .
Provides error/exception messaging display in JSP with conditional display of 
stacktraces on non-public sites. A public site does not display stacktraces and
is defined as site with hostname beginning with a value in the publicPrefixes
String array. ActionMessages are always displayed unless there's also a pageContext
exception or application exceptions.

Like the WDK's errors.tag, this tag handles:
- ActionMessages, e.g. form validation errors ( ${requestScope['org.apache.struts.action.ERROR']} )
- Application exceptions ( ${requestScope['org.apache.struts.action.EXCEPTION']} )
- JSP/jstl syntax errors ( ${pageContext.exception} )

Unlike the errors.tag, this tag does not display ActionMessages when there
is an application or JSP exception as the resulting ActionMessage text is garbage.

Also includes conditional error reporting via email. Email addresses are set
in the SITE_ADMIN_EMAIL property of the WDK's model.prop . Only public sites 
compose and send email reports.

- Mark Heiges <mheiges@uga.edu>, February 2010

Last edit $Id$
**/
package org.apidb.apicommon.taglib.wdk;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;

import org.apache.log4j.Logger;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.taglib.TagUtils;
import org.gusdb.wdk.model.WdkException;

public class ErrorsTag extends WdkTagBase {

    private String[] publicPrefixes = {
        "",
        "qa.",
        "beta.",
        "w1.",
        "w2.",
        "b1.",
        "b2.",
        "q1.",
        "q2.",
        "www."
    };
    private static final String PAGE_DIV = 
        "\n************************************************\n";
    private static final String FILTER_FILE = "/WEB-INF/wdk-model/config/errorsTag.filter";
    
    private PageContext pageContext;
    private HttpServletRequest request;
    private String showStacktrace;
    private String logMarker;
    private Logger logger = Logger.getLogger(getClass().getName());

    public ErrorsTag() {
    	super(PageContext.PAGE_SCOPE);
    }
    
    @Override
    public void doTag() throws JspException { 
        super.doTag();        
        pageContext = (PageContext) getJspContext();
        request = (HttpServletRequest) this.getRequest();
        
        if ( ! hasErrors() )
            return;
        
        logMarker = UUID.randomUUID().toString();
        String matchedFilterKey = filterMatch();
        StringBuffer logmsg = new StringBuffer();
        
        printActionErrorsToPage();
        printWdkExceptionErrorToPage();
                
        if (showStacktrace())
                printStackTraceToPage();
        
        if (siteIsMonitored()) {
            if (matchedFilterKey == null) {
                constructAndSendMail();
            } else {
                logmsg.append("\nError matches filter '" + matchedFilterKey + "'. No error report emailed.");
                writeFilteredErrorsToLog(matchedFilterKey);
            }
        }

        logger.error(logMarker + logmsg);
        
    }
    
    /**
     Check for matches to filters.
     Filters are regular expressions in a property file.
     The file is optional. In which case, no filtering is performed.
    
     Matches are checked against the text of errors and stacktraces.

     Property file example 1. A simple check for missing step ids.

         noStepForUser = The Step #\\d+ of user .+ doesn't exist
    
     
     Compound filtering can be configured with specific subkeys in 
     the property file (the primary key is always required).
     
     Property file example 2. Filter when exceptions contain the words 
     "twoPartName is null" and also the referer is empty.

        twoPartNameIsNull = twoPartName is null
        twoPartNameIsNull.referer = 

     Allowed subkeys are
        referer
        ip
    **/
    private String filterMatch() throws JspException {
    
        Properties filters = new Properties();
        PageContext pageContext = (PageContext) getJspContext();
        ServletContext app = pageContext.getServletContext();
        InputStream is = app.getResourceAsStream(FILTER_FILE);
        
        if (is == null) return null;
        
        try {
            filters.load(is);
        } catch (IOException e) {
            throw new JspException(e);
        }
         
        StringBuffer allErrors = new StringBuffer();

        allErrors.append(getStackTraceAsText());
        allErrors.append(getActionErrorsAsHTML());
        
        if (allErrors != null) {
           Set<String> propertyNames = filters.stringPropertyNames();
           for (String key : propertyNames) {

                // don't check subkeys yet
                if (key.contains(".")) continue;

                String regex = filters.getProperty(key);
                Pattern p = Pattern.compile(regex);
                Matcher m = p.matcher(allErrors);
         
                if (m.find()) {
                    /**
                        Found match for primary filter. Now check
                        for additional matches from any subkey filters.
                        Return on first match.
                    **/
                    boolean checkedSubkeys = false;
                    String refererFilter = filters.getProperty(key + ".referer");
                    String ipFilter      = filters.getProperty(key + ".ip");

                    if (refererFilter != null) {
                        checkedSubkeys = true;
                        String referer = (request.getHeader("Referer") != null) ? 
                            request.getHeader("Referer") : "";
                        if (refererFilter.equals(referer))
                            return key + " = " + regex + " AND " 
                                    + key + ".referer = " + refererFilter;
                    }

                    if (ipFilter != null) {
                        checkedSubkeys = true;
                        String remoteHost = (request.getRemoteHost() != null) ? 
                            request.getRemoteHost() : "";
                        if (ipFilter.equals(remoteHost))
                            return key + " = " + regex + " AND " 
                                    + key + ".ip = " + ipFilter;
                    }

                    // subkeys were checked and no matches in subkeys,
                    // so match is not sufficient to filter
                    if  (checkedSubkeys) return null;
                    
                    // Otherwise no subkeys were checked (so primary
                    // filter match is sufficient)
                    return key + " = " + regex;
                }
            }
        }
        
        return null;
        
        /**
        TODO if warranted: fine-tuned filtering on stack trace
        StackTraceElement[] stackElements = rex.getStackTrace();
        stackElements[i].getFileName();
            .getLineNumber();
            .getClassName();        
        **/
    }
    public void setShowStacktrace(String showStacktrace) {
        this.showStacktrace = showStacktrace;
    }

    private boolean showStacktrace() {
        if (showStacktrace != null && showStacktrace.equals("false") ) {
            return false;
        }
        return ( ! isPublicSite() );
    }
    
    private boolean isPublicSite() {
        String project = this.getContext().getInitParameter("model");
        String serverName =  request.getServerName();
        for (String prefix : publicPrefixes) {
            if ( (prefix + project + ".org").equalsIgnoreCase(serverName) ) {
                return true;
            }
        }
        return false;
    }
    
    private boolean siteIsMonitored() {
        return isPublicSite();
    }

    private boolean hasErrors() throws JspException {
        Exception pex = pageContext.getException(); // e.g. jstl sytnax errors
        Exception rex = (Exception) request.
                getAttribute(Globals.EXCEPTION_KEY); // e.g. WDK exceptions
        Exception aex = (Exception) request.getAttribute("exception");
        ActionMessages messages = TagUtils.getInstance().
                            getActionMessages(pageContext, Globals.ERROR_KEY);
        
        return ( pex != null || rex != null || aex != null || ! messages.isEmpty() );
    }

    private void printActionErrorsToPage() throws JspException {
        /** ActionMessage will be set if there's a exception. Skip printing
           these since the content is meaningless (e.g. "???en_US.global.error.other???")
           and we're processing the exception separately **/
        Exception pex = pageContext.getException(); // e.g. jstl sytnax errors
        Exception rex = (Exception) request. 
                getAttribute(Globals.EXCEPTION_KEY); // e.g. WDK exceptions
        Exception aex = (Exception) request.getAttribute("exception");

        if (rex != null || pex != null || aex != null) return;

        String message = getActionErrorsAsHTML();
        
        if (message == null) return;

        try {
            JspWriter out = getJspContext().getOut();
            out.println("<br>");
            out.println("<EM><b>Please correct the following error(s): </b></EM><br>");
            out.println(message);
        } catch (IOException ioe) {
            throw new JspException("(IOException) " + ioe);
        }    
    }

    private void printStackTraceToPage() throws JspException {
        String st = getStackTraceAsText();
        if (st == null) return;
        try {
            JspWriter out = getJspContext().getOut();            
            out.println("<br>");
            out.println("<pre>\n");
            out.println(st);
            out.println("</pre>\n");
            out.println("log4j marker: " + logMarker);
        } catch (IOException ioe) {
            throw new JspException("(IOException) " + ioe);
        }
    }
    
    private void printWdkExceptionErrorToPage() throws JspException {
        Exception rex = (Exception) request.
                getAttribute(Globals.EXCEPTION_KEY); // e.g. WDK exceptions
        
        if (rex != null && rex instanceof WdkException) {        
            try {
                JspWriter out = getJspContext().getOut();
                out.println("<br>");
                out.println("<pre>\n");
                out.println(((WdkException) rex).formatErrors());
                out.println("</pre>\n");
            } catch (IOException ioe) {
                throw new JspException("(IOException) " + ioe);
            }
        }
    }
    /**
        Equivalent of 
            
                <UL>
                    <html:messages id="error" message="false">
                        <LI><bean:write name="error"/></LI>
                    </html:messages>
                </UL>
        
        Based on MessagesTag.java from Struts 1.2.4 
        http://grepcode.com/file/repo1.maven.org/maven2/struts/struts/1.2.4/org/apache/struts/taglib/html/MessagesTag.java/?v=source
    **/
    private String getActionErrorsAsHTML() throws JspException {
        
        /** MessagesTag attributes **/
        String message  = "false";
        String bundle   = null;
        String property = null;
        //String header   = null;
        //String footer   = null;
        String locale   = Globals.LOCALE_KEY;
        String name     = Globals.ERROR_KEY;
        
        StringBuffer sb = new StringBuffer();
        
        if (message != null && message.equalsIgnoreCase("true")) {
            name = Globals.MESSAGE_KEY;
        }

        /* dead code for now 
        if (header != null && header.length() > 0) {
            String headerMessage = TagUtils.getInstance().
                    message(pageContext, bundle, locale, header);
            if (headerMessage != null) {
                sb.append(headerMessage);
            }
        }
        */
        
        ActionMessages messages = TagUtils.getInstance().
                            getActionMessages(pageContext, name);

        Iterator<?> i = (property == null) ? messages.get() : messages.get(property);

        if (! i.hasNext()) return null;
        
        sb.append("<ul>\n");
        while (i.hasNext()) {
        
            ActionMessage report = (ActionMessage) i.next();
            String msg = TagUtils.getInstance().message(
                         pageContext, bundle, locale,
                         report.getKey(), report.getValues());
            
            if (msg != null) {
                sb.append("<li>" + msg + "</li>\n");
            }
        }
        sb.append("</ul>\n");

        /* dead code for now 
        if (footer != null && footer.length() > 0) {
            String footerMessage = TagUtils.getInstance().
                    message(pageContext, bundle, locale, footer);
            if (footerMessage != null) {
                sb.append(footerMessage);
            }
        }
        */
        
        return sb.toString();
    }
    

    private String getStackTraceAsText() {
        Exception pex = pageContext.getException(); // e.g. jstl sytnax errors
        Exception rex = (Exception) request.
                getAttribute(Globals.EXCEPTION_KEY); // e.g. WDK exceptions
        Exception aex = (Exception) request.getAttribute("exception");

        if (pex == null && rex == null && aex == null) 
            return null;
        
        StringBuffer st = new StringBuffer();

        if (rex != null) {
            st.append(stackTraceToString(rex));
            st.append("\n\n-- from pageContext.getException()\n");
        }
        if (pex != null) {
            st.append(stackTraceToString(pex));
            st.append("\n\n-- from request.getAttribute(Globals.EXCEPTION_KEY)\n");
        }
        if (aex != null) {
            st.append(stackTraceToString(aex));
            st.append("\n\n-- from request.getAttribute(\"exception\")\n");
        }
        return st.toString();
    }
    
    private String stackTraceToString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
    
    private String getEmailBody() {
        StringBuffer body = new StringBuffer();
        
        appendErrorUrl(body);
        appendRemoteHost(body);
        appendReferer(body);
        
        appendUserAgent(body);
        appendServerAddress(body);

        body.append(PAGE_DIV);
        appendRequestParameters(body);
        
        body.append(PAGE_DIV);
        appendRequestScopeAttributes(body);
        
        body.append(PAGE_DIV);
        appendSessionAttributes(body);
        
        /**
        body.append(PAGE_DIV);
        appendServletContextAttributes(body);
        **/
        body.append(PAGE_DIV);
        body.append("log4j marker: " + logMarker);

        body.append(PAGE_DIV);
        appendStacktrace(body);
        
        body.append("\n\n");
        
        return body.toString();
    }
    
    private String getEmailSubject() {
        String project = this.getContext().getInitParameter("model");
        String sbj = project + " Site Error" + " - " + request.getRemoteHost();
        return sbj;
    }
    
    private void constructAndSendMail() {
                
        Map<String, String> modelProps = getWdkModel().getProperties();
        String adminProp = modelProps.get("SITE_ADMIN_EMAIL");

        if (adminProp == null) {
            Logger.getLogger(getClass().getName()).
                error("SITE_ADMIN_EMAIL is not configured in model.prop; cannot send exception report.");
            return;
        }
        
        String[] emailList = Pattern.compile("[,\\s]+").split(adminProp);
    
        String[] email = emailList;
        String from = "tomcat@" + request.getServerName();
        String subject = getEmailSubject();

        String message = getEmailBody();

        sendMail(email, from, subject, message.toString());
    }
    
    private void appendStacktrace(StringBuffer sb) {
        sb.append("Stacktrace: \n\n");
        String st = getStackTraceAsText();
        if (st == null) return;
        sb.append(st);
    }
    
    private void appendRemoteHost(StringBuffer sb) {
        String remoteHost = (request.getRemoteHost() != null) ? 
            request.getRemoteHost() : "<not set>";
        sb.append("Remote Host: " + remoteHost + "\n");
    }
    
    private void appendReferer(StringBuffer sb) {
        String referer = (request.getHeader("Referer") != null) ? 
            request.getHeader("Referer") : "<not set>";
        sb.append("Referred from: " + referer + "\n");
    }

    private void appendErrorUrl(StringBuffer sb) {
        //String queryString = (String)request.getAttribute("javax.servlet.forward.query_string");
        StringBuffer errorUrl = new StringBuffer();
        String currentRequestURI = currentRequestURI();
        if (currentRequestURI == null || currentRequestURI.equals("null")) {
            errorUrl.append("<unable to determine request URI>");
        } else {
            errorUrl.append("\n  " + request.getScheme() + "://" + request.getServerName());
            errorUrl.append(currentRequestURI);
        }
        sb.append("Error on: " + errorUrl + "\n");
    }

    private String currentRequestURI() {
        String currentRequestURI = "";
        
        String queryString = "?" + (String)request.getAttribute("javax.servlet.forward.query_string");
        String requestURI = (String)request.getAttribute("javax.servlet.forward.request_uri");
        
        if (requestURI == "null") {
             return null;
        }


        if (queryString.equals("?null")) 
            queryString = "";
        
        currentRequestURI = requestURI + queryString;

        return currentRequestURI;
    }
    
    private void appendUserAgent(StringBuffer sb) {
        String userAgent = request.getHeader("user-agent");
        sb.append("UserAgent: " + "\n  " + userAgent + "\n");
    }
    
    private void appendServerAddress(StringBuffer sb) {
        // "JkEnvVar SERVER_ADDR" is required in Apache configuration
        String serverAddr = (request.getAttribute("SERVER_ADDR") != null) ?
            (String)request.getAttribute("SERVER_ADDR") :
            "<not set; is 'JkEnvVar SERVER_ADDR' set in the Apache configuration?>";
        sb.append("Server Addr: " + serverAddr + "\n");
    }
    
    @SuppressWarnings("unchecked")
    private void appendRequestParameters(StringBuffer sb) {
        sb.append("Request Parameters (request to the server)\n\n");
        Map<String,String[]> parameters = request.getParameterMap(); 
        
        for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
            for (String value : entry.getValue()) {
                sb.append(entry.getKey() + " = " + value + "\n");
            }
        }
    }
    
    @SuppressWarnings("unused")
    private void appendServletContextAttributes(StringBuffer sb) {
        sb.append("ServletContext Attributes\n\n");
        ServletContext context = this.getContext();
        Enumeration<?> contextAttributes = context.getAttributeNames();
        while (contextAttributes.hasMoreElements()) {
            String attr = (String)contextAttributes.nextElement();
            String value = context.getAttribute(attr).toString();
            sb.append(attr + " = " + value + "\n");
        }       
    }

    private void appendRequestScopeAttributes(StringBuffer sb) {
        sb.append("Associated Request-Scope Attributes\n\n");
        Enumeration<?> requestScope = request.getAttributeNames();
        while (requestScope.hasMoreElements()) {
            String attr = (String)requestScope.nextElement();
            String value = (attr.toLowerCase().startsWith("email") ||
                            attr.toLowerCase().startsWith("passw")
                           ) 
                 ? "*****" 
                 : request.getAttribute(attr).toString();
            sb.append(attr + " = " + value + "\n");
        }
    }
    
    private void appendSessionAttributes(StringBuffer sb) {
        sb.append("Session Attributes\n\n");
        HttpSession session = request.getSession();
        if (session != null) {
        	Enumeration<?> attributeNames = session.getAttributeNames();
            while (attributeNames.hasMoreElements()) {
            	String name = (String)attributeNames.nextElement();
                sb.append(name + " = " + session.getAttribute(name) + "\n");
            }
        }
    }
    
    private void sendMail( String recipients[], String from, String subject, 
                           String message) {
    
        if (recipients.length == 0) return;
        
        try {
           Properties props = new Properties();
           props.put("mail.smtp.host", "localhost");
      
          Session session = Session.getDefaultInstance(props, null);
          session.setDebug(false);
      
          Message msg = new MimeMessage(session);
          InternetAddress addressFrom = new InternetAddress(from);
          msg.setFrom(addressFrom);
      
          List <InternetAddress> addressList = new ArrayList<InternetAddress>();
          for (int i = 0; i < recipients.length; i++) {
              try {
                  addressList.add(new InternetAddress(recipients[i]));
              } catch (AddressException ae) {
                   //ignore bad address
              }
          }
          InternetAddress[] addressTo = addressList.toArray(new InternetAddress[0]);
          
          msg.setRecipients(Message.RecipientType.TO, addressTo);
      
          msg.setSubject(subject);
          msg.setContent(message, "text/plain");
          Transport.send(msg);
      
        } catch (MessagingException me) {
             Logger.getLogger(getClass().getName()).error(me);
        }
    }
    
    // archive the errors that were not emailed due to matched filter
    private void writeFilteredErrorsToLog(String matchedFilterKey) {

        String filteredLogDirName = System.getProperty("catalina.base") + "/logs/filtered_errors";
        File filteredLogDir = new File(filteredLogDirName);
        if ( ! filteredLogDir.exists())
            filteredLogDir.mkdir();
            
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
            String remoteHost = (request.getRemoteHost() != null) ? 
                            request.getRemoteHost() : "_";
            String logName = remoteHost + "-" + sdf.format(new Date());

            FileWriter fw = new FileWriter(filteredLogDirName + "/" + logName, true);
            BufferedWriter out = new BufferedWriter(fw);

            String from = "tomcat@" + request.getServerName();
            String subject = getEmailSubject();    
            String message = getEmailBody();

            out.write("Filter Match: " + matchedFilterKey + "\n");
            out.write("Subject: " + subject + "\n");
            out.write("From: " + from + "\n");
            out.write(message + "\n");
            out.write("\n//\n");
            out.close();
        } catch (Exception e) {
            logger.error(e);
        }
    }

}
