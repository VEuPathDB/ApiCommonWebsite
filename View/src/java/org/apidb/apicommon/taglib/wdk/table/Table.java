package org.apidb.apicommon.taglib.wdk.table;

import javax.servlet.jsp.JspException;
import java.io.IOException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.util.Map;
import org.gusdb.wdk.model.TableValue;
import org.gusdb.wdk.model.jspwrap.RecordBean;
import org.gusdb.wdk.controller.CConstants;

public class Table extends SimpleTagSupport {
    private String var;
    private TableValue tableFieldValue;
    public String tableName;

    public void doTag() throws JspException, IOException {
        setTableFieldValue();
        getJspContext().setAttribute(var,tableFieldValue);
        getJspBody().invoke(null);
    }
    
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
    
    public void setVar(String var) {
        this.var = var;
    }

    private void setTableFieldValue() throws JspException {
        try {
            Map tables = getRecord().getTables();
            tableFieldValue = (TableValue)tables.get(tableName);
        } catch (Exception e) {
            throw new JspException(e);
        }
    }
    
    protected TableValue getTableFieldValue() {
        return tableFieldValue;
    }
    
    private RecordBean getRecord() {
        return ((RecordBean)(
            (PageContext)getJspContext()).
                getRequest().
                  getAttribute(CConstants.WDK_RECORD_KEY));
    }

}
