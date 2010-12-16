package org.apidb.apicommon.taglib.wdk;

import java.util.ArrayList;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.ValidationMessage;

public class SiteXmlMessagesTEI extends TagExtraInfo {

    private ValidationMessage[] vmsg = null;

    public ValidationMessage[] validate(TagData data) {

        ArrayList errors = new ArrayList();
        
        validateRange(errors, data.getAttribute("range"));
        validateStopDateSort(errors, data.getAttribute("stopDateSort"));
        
        if (errors.size() != 0) {
            vmsg = new ValidationMessage[errors.size()];
            errors.toArray(vmsg);
        }
    
        return vmsg;
    }
        
    public void validateRange(ArrayList errors, Object data) {

        if (data == null)
            return;

        String range = ((String)data).toLowerCase();

        if ( ! range.equals("expired") && 
             ! range.equals("all") ) {
            errors.add(
                new ValidationMessage(null,
                "Invalid range value. Valid values are ['expired', 'all']")
            );
        }
        
        if (errors.size() != 0) {
            vmsg = new ValidationMessage[errors.size()];
            errors.toArray(vmsg);
        }
    
    }
        
    public void validateStopDateSort(ArrayList errors, Object data) {

        if (data == null)
            return;
    
        String stopDateSort = ((String)data).toLowerCase();

        if ( ! stopDateSort.equals("asc") && 
             ! stopDateSort.equals("desc") ) {
            errors.add(
                new ValidationMessage(null,
                "Invalid stopDateSort value. Valid values are ['ASC', 'DESC']")
            );
        }
    
        if (errors.size() != 0) {
            vmsg = new ValidationMessage[errors.size()];
            errors.toArray(vmsg);
        }
    
    }
}