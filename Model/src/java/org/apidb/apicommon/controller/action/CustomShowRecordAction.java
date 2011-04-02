package org.apidb.apicommon.controller.action;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.gusdb.wdk.controller.CConstants;
import org.gusdb.wdk.controller.action.ActionUtility;
import org.gusdb.wdk.controller.action.ShowRecordAction;
import org.gusdb.wdk.model.AttributeValue;
import org.gusdb.wdk.model.TableValue;
import org.gusdb.wdk.model.jspwrap.AnswerValueBean;
import org.gusdb.wdk.model.jspwrap.QuestionBean;
import org.gusdb.wdk.model.jspwrap.RecordBean;
import org.gusdb.wdk.model.jspwrap.UserBean;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;

public class CustomShowRecordAction extends ShowRecordAction {
    
    private static final String PARAM_RECORD_CLASS = "record_class";
    private static final String TABLE_REFERENCE = "References";
    private static final String TYPE_ATTRIBTUE = "attribute";
    private static final String TYPE_TABLE = "table";
    
    private static final String ATTR_REFERENCE_ATTRIBUTES = "ds_ref_attributes";
    private static final String ATTR_REFERENCE_TABLES = "ds_ref_tables";
    
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        // run execute from parent
        ActionForward forward = super.execute(mapping, form, request, response);

        // load the recordClass based data sources
        WdkModelBean wdkModel = ActionUtility.getWdkModel(servlet);
        UserBean user = ActionUtility.getUser(servlet, request);
        RecordBean record = (RecordBean) request
                .getAttribute(CConstants.WDK_RECORD_KEY);
        String rcName = record.getRecordClass().getFullName();

        // get the data source question
        QuestionBean question = wdkModel
                .getQuestion(GetDataSourceAction.DATA_SOURCE_BY_RECORD_CLASS);
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put(PARAM_RECORD_CLASS, rcName);
        AnswerValueBean answerValue = question.makeAnswerValue(user, params, 0);

        // find all referenced attributes and tables;
        Map<String, String> attributeRefs = new LinkedHashMap<String, String>();
        Map<String, String> tableRefs = new LinkedHashMap<String, String>();
        Iterator<RecordBean> dsRecords = answerValue.getRecords();
        while(dsRecords.hasNext()) {
            RecordBean dsRecord = dsRecords.next();
            TableValue tableValue = dsRecord.getTables().get(TABLE_REFERENCE);
            for(Map<String, AttributeValue> row : tableValue) {
                String recordType = row.get("record_type").toString();
                if (recordType.equals(rcName)) {
                    String targetType = row.get("target_type").toString();
                    String targetName = row.get("target_name").toString();
                    if (targetType.equals(TYPE_ATTRIBTUE)) {
                        attributeRefs.put(targetName, targetName);
                    } else if (targetType.equals(TYPE_TABLE)) {
                        tableRefs.put(targetName, targetName);
                    }
                }
            }
        }
        
        request.setAttribute(ATTR_REFERENCE_ATTRIBUTES, attributeRefs);
        request.setAttribute(ATTR_REFERENCE_TABLES, tableRefs);

        return forward;
    }

}
