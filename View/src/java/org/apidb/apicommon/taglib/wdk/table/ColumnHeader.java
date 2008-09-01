package org.apidb.apicommon.taglib.wdk.table;

import javax.servlet.jsp.JspException;
import java.io.IOException;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.util.Iterator;
import java.util.Map.Entry;
import org.gusdb.wdk.model.AttributeField;

public class ColumnHeader extends SimpleTagSupport {
    private Iterator iterator;
    private String var;

    public void doTag() throws JspException, IOException {
        Table wdkTable = (Table)findAncestorWithClass(
            this, Table.class);

        AttributeField hCol[] = wdkTable.getTableFieldValue().getDisplayableFields();
        
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
