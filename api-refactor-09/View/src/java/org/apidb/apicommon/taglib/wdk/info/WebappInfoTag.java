/**
*
* Provides information about the running webapp
*
*/

package org.apidb.apicommon.taglib.wdk.info;


import javax.servlet.jsp.tagext.SimpleTagSupport;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletRequest;
import javax.servlet.ServletContext;
import java.util.Calendar;
import java.util.Vector;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.w3c.dom.Document;
import org.w3c.dom.*;
import org.apache.xerces.parsers.DOMParser;
import org.apidb.apicommon.controller.ApiCommonConstants;
import org.apache.log4j.Logger;

public class WebappInfoTag extends SimpleTagSupport {

    private String var;
    private String dateFormatStr="EEE dd MMM yyyy h:mm a";
    private PageContext pageContext;
    private ServletContext application;
    private Document statefile;
    private String stateFilePath;
    private String instanceUptimeText;
    private String webappUptimeText;
    private Logger logger = Logger.getLogger(WebappInfoTag.class);
    
    public void doTag() throws JspException {        
        pageContext = (PageContext) getJspContext();
        application = pageContext.getServletContext();

        File tempdir = (File)application.getAttribute("javax.servlet.context.tempdir");
        stateFilePath = tempdir + "/" + ApiCommonConstants.WEBAPP_START_STATE_FILE;
        statefile = parseStateFile();
        
        setInstanceUptimeText();
        setWebappUptimeText();
        
        this.getRequest().setAttribute(var, this);
    }
    
    public String getInstanceUptimeText() {
        return instanceUptimeText;
    }
    
    public String getWebappUptimeText() {
        return webappUptimeText;
    }
    
    /**
     * Returns the name of the Tomcat instance as set in the System property instance.name 
     * at instance startup. This is typically defined in the startup script (e.g. instance_manager).
     *
     * @return the value of the instance.name System property for the Tomcat instance serving the page.
     */
    public String getInstanceName() {
        return System.getProperty("instance.name");
    }
    
    /**
    * Same return value as ${pageContext.request.contextPath}
    */
    public String getContextPath() {
       return ((HttpServletRequest)getRequest()).getContextPath();
    }
    
    /**
    * Same return value as ${applicationScope['org.apache.catalina.jsp_classpath']}
    */
    public String getClasspath() {
        return (String) application.getAttribute("org.apache.catalina.jsp_classpath");
    }
        
    public void setVar(String var) {
        this.var = var;
    }

    private String uptimeBrief(int days, int hours, int minutes, int seconds) {
      String uptimeBrief = "";
      if (days != 0)
        uptimeBrief = days + "d " + hours + "h";
      else if (hours != 0)
        uptimeBrief = hours + "h " + minutes + "m";
      else if (seconds != 0)
        uptimeBrief = minutes + "m " + seconds + "s";
      
      return uptimeBrief;
    }

    private String elapsedTimeSinceTomcatJVMStart() throws Exception {
      try {
        String result;
        Vector commands=new Vector();
        commands.add("/bin/bash");
        commands.add("-c");
        commands.add("ps -o etime $PPID | tail -n1");
        
        ProcessBuilder pb=new ProcessBuilder(commands);  
        Process pr=pb.start();
        pr.waitFor();
        
        if (pr.exitValue()==0) {
            BufferedReader output = new BufferedReader(
                            new InputStreamReader(pr.getInputStream()));
            result = output.readLine().trim();
            output.close();
        } else {
            BufferedReader error = new BufferedReader(
                            new InputStreamReader(pr.getErrorStream()));        
            result = "Error: " + error.readLine(); 
        }
        return result;
        } catch (Exception e) {
        throw e;
      }
    }

    private String uptimeSince(int days, int hours, int minutes, int seconds, String fmt) {
      Calendar calendar = java.util.Calendar.getInstance();
       
      calendar.add(Calendar.DAY_OF_MONTH, -days);
      calendar.add(Calendar.HOUR,         -hours);
      calendar.add(Calendar.MINUTE,       -minutes);
      calendar.add(Calendar.SECOND,       -seconds);
      
      java.util.Date startTime = calendar.getTime();
    
      java.text.DateFormat formatter = new java.text.SimpleDateFormat(fmt);
    
      return (String)formatter.format(startTime) ;
    }

    private void setInstanceUptimeText() {
      try {
    
        String uptime = elapsedTimeSinceTomcatJVMStart();
        uptime = uptime.trim();
        
        java.util.regex.Pattern pat;
        java.util.regex.Matcher m;
    
        int days    = 0;
        int hours   = 0;
        int minutes = 0;
        int seconds = 0;
     
        pat = java.util.regex.Pattern.compile("^(?:(\\d+)-)?(?:(\\d+):)?(\\d+):(\\d+)$");
        m = pat.matcher(uptime);
    
        if (m.find()) {
          if (m.group(1) != null) days    = Integer.parseInt(m.group(1));
          if (m.group(2) != null) hours   = Integer.parseInt(m.group(2));
          if (m.group(3) != null) minutes = Integer.parseInt(m.group(3));
          if (m.group(4) != null) seconds = Integer.parseInt(m.group(4));
        } else {
            throw new Exception();
        }
            
        String uptimeSince = uptimeSince(days, hours, minutes, seconds, dateFormatStr);
        String uptimeBrief = uptimeBrief(days, hours, minutes, seconds);
        
        instanceUptimeText = uptimeBrief + " (since " + uptimeSince + ")";
    
      } catch (Exception e) {
        instanceUptimeText = "<font color='red'>Error: unable to determine start time" + e + "</font>";
      }
    }

    private void setWebappUptimeText() {
      try {
        java.text.DateFormat formatter = new java.text.SimpleDateFormat(dateFormatStr);
        
        if (statefile == null) {
            webappUptimeText = "<i>unknown (check webapp logs for failures of " 
              + this.getClass().getName() + ")</i>";
            return;
        }
        
        Node stNode = statefile.getElementsByTagName("starttime").item(0);
        String starttimeString = stNode.getFirstChild().getNodeValue();

        SimpleDateFormat format = new SimpleDateFormat(ApiCommonConstants.ISO8601_DATE_FORMAT);
        Date starttimeDate = format.parse(starttimeString);

        long milliseconds = System.currentTimeMillis() - starttimeDate.getTime();
         
        int days    = (int)(milliseconds / (1000*60*60*24))     ;
        int hours   = (int)(milliseconds / (1000*60*60   )) % 24;
        int minutes = (int)(milliseconds / (1000*60      )) % 60;
        int seconds = (int)(milliseconds / (1000         )) % 60;

        String uptimeSince = (String)formatter.format(starttimeDate);
        String uptimeBrief = uptimeBrief(days, hours, minutes, seconds);
       
        webappUptimeText = uptimeBrief + " (since " + uptimeSince + ")";
      } catch (Exception e) {
        webappUptimeText = "Error: " + e;
      }
    }

    private ServletRequest getRequest() {
        return ((PageContext)getJspContext()).getRequest();
    }

    private ServletContext getContext() {
        return ((PageContext)getJspContext()).
                  getServletConfig().getServletContext();
    }
    
    private Document parseStateFile() {
        DOMParser parser = new DOMParser();        
        try {
          parser.parse(stateFilePath);
        } catch (org.xml.sax.SAXException se) {
            logger.warn("Error parsing XML in " + stateFilePath + " : " + se);
            return null;
        } catch (java.io.IOException ioe) {
            logger.warn("Error: " + ioe);
            return null;
        }
        return parser.getDocument();
    }
}
