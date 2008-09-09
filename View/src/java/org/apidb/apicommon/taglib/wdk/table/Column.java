package org.apidb.apicommon.taglib.wdk.table;

import javax.servlet.jsp.JspException;
import java.io.IOException;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.util.Iterator;
import	java.util.Map.Entry;

public class Column extends SimpleTagSupport {
    private Iterator iterator;
    private String var;

    public void doTag() throws JspException, IOException {
        Row row = (Row)findAncestorWithClass(
            this, Row.class);

        iterator = row.getRow().entrySet().iterator();
        
        if (iterator == null) {
            return;
        }
        while (iterator.hasNext()) {
            Entry col = (Entry)iterator.next();
            getJspContext().setAttribute(var, col.getValue());
            getJspBody().invoke(null);
        }
        
    }
    
    public void setVar(String var) {
        this.var = var;
    }

}
