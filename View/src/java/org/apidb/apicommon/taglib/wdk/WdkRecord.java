/**

Create a WDK record of given 'name' and optional 'primaryKey' and
optional 'projectID'. primaryKey defaults to primaryKey in model. 
projectID defaults to a single space.

Modeled after org.gusdb.wdk.controller.action.ShowRecordAction.

Usage in a JSP document:
  <%@ taglib prefix="api" uri="http://apidb.org/taglib" %>
  <api:wdkRecord name="UtilityRecordClasses.SiteInfo" />
  <c:set var="attrs" value="${wdkRecord.attributes}"/>
  ${attrs['primaryKey'].value}

**/

package org.apidb.apicommon.taglib.wdk;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;
import javax.servlet.ServletContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import javax.servlet.jsp.JspException;

import org.gusdb.wdk.controller.CConstants;
import org.gusdb.wdk.model.jspwrap.RecordBean;
import org.gusdb.wdk.model.jspwrap.RecordClassBean;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.WdkModelException;

public class WdkRecord extends SimpleTagSupport {

    private String name;
    private String projectID;
    private String primaryKey;
    private String recordKey;
    
    public void doTag() throws JspException {
        RecordBean wdkRecord = getRecord();
        if (recordKey == null) recordKey = CConstants.WDK_RECORD_KEY;
        getRequest().setAttribute(recordKey, wdkRecord);
    }

    private RecordBean getRecord() throws JspException {
    
        WdkModelBean wdkModel = (WdkModelBean) getContext().
                getAttribute(CConstants.WDK_MODEL_KEY);
                
        try {
            if (projectID == null) projectID = wdkModel.getProjectId();
            if (primaryKey == null) primaryKey = " ";

            RecordClassBean wdkRecordClass = wdkModel.
                findRecordClass(name);
            
            RecordBean wdkRecord = wdkRecordClass.
                makeRecord(projectID, primaryKey);
            
            return wdkRecord;

        } catch (WdkUserException wue) {
            throw new JspException(wue);
        } catch (WdkModelException wme) {
            throw new JspException(wme);
        }

    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProjectID(String projectID) {
        this.projectID = projectID;
    }

    public void setPrimaryKey(String primaryKey) {
        this.primaryKey = primaryKey;
    }

    public void setRecordKey(String recordKey) {
        this.recordKey = recordKey;
    }

    private ServletRequest getRequest() {
        return ((PageContext)getJspContext()).getRequest();
    }
    
    private ServletContext getContext() {
        return ((PageContext)getJspContext()).
                  getServletConfig().getServletContext();
    }
    

}