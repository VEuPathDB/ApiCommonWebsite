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

$Id$

**/
package org.apidb.apicommon.taglib.wdk;

import org.apache.struts.taglib.TagUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.Globals;
import org.apache.struts.util.MessageResources;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.UUID;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;

import org.apache.log4j.Logger;


public class ErrorsTag extends WdkTagBase {

    private String[] publicPrefixes = {
        "",
        "qa.",
        "beta.",
        "w1.",
        "w2.",
        "b1.",
        "b2.",
        "www."
    };
    private static final String PAGE_DIV = 
        "\n************************************************\n";


    private PageContext pageContext;
    private HttpServletRequest request;
    protected int varScope;
    private String showStacktrace;
    private String logMarker;

    public ErrorsTag() {
        varScope = PageContext.PAGE_SCOPE;
    }
    
    
    public void doTag() throws JspException { 
        super.doTag();        
        pageContext = (PageContext) getJspContext();
        request = (HttpServletRequest) this.getRequest();
        
        if ( ! hasErrors() )
            return;
        
        logMarker = UUID.randomUUID().toString();
        Logger.getLogger(getClass().getName()).error(logMarker);

        printActionErrorsToPage();

        if (showStacktrace())
            printStackTraceToPage();
        
        if (siteIsMonitored())
            constructAndSendMail();

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
        String header   = null;
        String footer   = null;
        String locale   = Globals.LOCALE_KEY;
        String name     = Globals.ERROR_KEY;
        
        StringBuffer sb = new StringBuffer();
        
        if (message != null && message.equalsIgnoreCase("true")) {
            name = Globals.MESSAGE_KEY;
        }

        if (header != null && header.length() > 0) {
            String headerMessage = TagUtils.getInstance().
                    message(pageContext, bundle, locale, header);
            if (headerMessage != null) {
                sb.append(headerMessage);
            }
        }

        ActionMessages messages = TagUtils.getInstance().
                            getActionMessages(pageContext, name);

        Iterator i = (property == null) ? messages.get() : messages.get(property);

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
        
        if (footer != null && footer.length() > 0) {
            String footerMessage = TagUtils.getInstance().
                    message(pageContext, bundle, locale, footer);
            if (footerMessage != null) {
                sb.append(footerMessage);
            }
        }

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
                
        Map<String, String> modelProps = wdkModel.getProperties();
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
        String queryString = (String)request.getAttribute("javax.servlet.forward.query_string");
        StringBuffer errorUrl = new StringBuffer();
        errorUrl.append(request.getScheme() + "://" + request.getServerName());
        errorUrl.append(request.getAttribute("javax.servlet.forward.request_uri"));
        if (queryString != null) 
            errorUrl.append("?" + queryString);

        sb.append("Error on: " + "\n  " + errorUrl + "\n");
    }

    private void appendUserAgent(StringBuffer sb) {
        String userAgent = (String)request.getHeader("user-agent");
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
    
    private void appendServletContextAttributes(StringBuffer sb) {
        sb.append("ServletContext Attributes\n\n");
        ServletContext context = this.getContext();
        Enumeration contextAttributes = context.getAttributeNames();
        while (contextAttributes.hasMoreElements()) {
            String attr = (String)contextAttributes.nextElement();
            String value = context.getAttribute(attr).toString();
            sb.append(attr + " = " + value + "\n");
        }       
    }

    private void appendRequestScopeAttributes(StringBuffer sb) {
        sb.append("Associated Request-Scope Attributes\n\n");
        Enumeration requestScope = request.getAttributeNames();
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
            for (String name : session.getValueNames()) {
                sb.append(name + " = " + session.getValue(name) + "\n");
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


}
