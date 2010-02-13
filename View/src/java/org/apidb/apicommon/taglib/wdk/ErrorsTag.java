package org.apidb.apicommon.taglib.wdk;


import javax.servlet.http.HttpSession;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletRequest;
import javax.servlet.ServletContext;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.Enumeration;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;


public class ErrorsTag extends WdkTagBase {

    private PageContext pageContext;
    private HttpServletRequest request;
    private static final String PAGE_DIV = "\n************************************************\n";
    protected int varScope;
        


    public ErrorsTag() {
        varScope = PageContext.PAGE_SCOPE;
    }
    
    
    public void doTag() throws JspException { 
        super.doTag();        
        pageContext = (PageContext) getJspContext();
        request = (HttpServletRequest) this.getRequest();

        if (showStackTrace())
            printStackTraceToPage();
        
        if (siteIsMonitored())
            constructAndSendMail();

    }
    
    private void printStackTraceToPage() throws JspException {
        try {
            JspWriter out = getJspContext().getOut();            
            out.println("<pre>");
            out.println(getStackTraceAsText());
            out.println("</pre>");
        } catch (IOException ioe) {
            throw new JspException("(IOException) " + ioe);
        }
    }
    
    private boolean showStackTrace() {
        return true;
    }
    
    private boolean siteIsMonitored() {
        return true;
    }

    private String getStackTraceAsText() {
        Exception pex = pageContext.getException(); // e.g. jstl sytnax errors
        Exception rex = (Exception) request.
                getAttribute("org.apache.struts.action.EXCEPTION"); // e.g. WDK exceptions
        
        StringBuffer st = new StringBuffer();

        if (rex != null)
            st.append(stackTraceToString(rex));
        if (pex != null)
            st.append(stackTraceToString(pex));
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
        sb.append(getStackTraceAsText());
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
        String errorUrl = request.getScheme()
            + "://" + request.getServerName()
            + request.getAttribute("javax.servlet.forward.request_uri") 
            + "?" + request.getAttribute("javax.servlet.forward.query_string");

        sb.append("Error on: " + "\n" + errorUrl + "\n");
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