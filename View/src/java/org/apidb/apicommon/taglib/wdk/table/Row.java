package org.apidb.apicommon.taglib.wdk.table;

import javax.servlet.jsp.JspException;
import java.io.IOException;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.util.Map;
import java.util.Iterator;
import org.gusdb.wdk.model.TableValue;

public class Row extends SimpleTagSupport {
    private Iterator iterator;
    private String var;
    private Map row;

    public void doTag() throws JspException, IOException {
        Table wdkTable = (Table)findAncestorWithClass(
            this, Table.class);

        iterator = wdkTable.getTableFieldValue().iterator();
        
        if (iterator == null) {
            return;
        }
        
        while (iterator.hasNext()) {
            row = (Map)iterator.next();
            getJspContext().setAttribute(var, row);
            getJspBody().invoke(null);
        }
        
    }

    public void setVar(String var) {
        this.var = var;
    }
    
    protected Map getRow() {
        return row;
    }
}

