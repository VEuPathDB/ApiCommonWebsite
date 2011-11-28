package org.apidb.apicommon.taglib.wdk.table;

import javax.servlet.jsp.JspException;
import java.io.IOException;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.util.Iterator;
import java.util.Map.Entry;
import org.gusdb.wdk.model.AttributeField;

public class ColumnHeaderTag extends SimpleTagSupport {
    private Iterator iterator;
    private String var;

    public void doTag() throws JspException, IOException {
        TableTag wdkTable = (TableTag)findAncestorWithClass(
            this, TableTag.class);

        AttributeField hCol[] = wdkTable.getTableFieldValue().getTableField().getAttributeFields();
        
        if (hCol.length == 0) {
            return;
        }
        for  (AttributeField af : hCol) {
            getJspContext().setAttribute(var, af);
            getJspBody().invoke(null);
        }
        
    }
    
    public void setVar(String var) {
        this.var = var;
    }

}
