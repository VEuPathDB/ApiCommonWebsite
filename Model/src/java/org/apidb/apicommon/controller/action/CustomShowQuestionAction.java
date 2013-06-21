package org.apidb.apicommon.controller.action;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.gusdb.wdk.controller.CConstants;
import org.gusdb.wdk.controller.action.ShowQuestionAction;
import org.gusdb.wdk.controller.actionutil.ActionUtility;
import org.gusdb.wdk.model.jspwrap.AnswerValueBean;
import org.gusdb.wdk.model.jspwrap.QuestionBean;
import org.gusdb.wdk.model.jspwrap.RecordBean;
import org.gusdb.wdk.model.jspwrap.UserBean;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;
import org.gusdb.wdk.model.record.TableValue;
import org.gusdb.wdk.model.record.attribute.AttributeValue;

public class CustomShowQuestionAction extends ShowQuestionAction {

    private static final String PARAM_QUESTION = "question_name";
    private static final String TABLE_REFERENCE = "References";
    private static final String TYPE_QUESTION = "question";

    private static final String ATTR_REFERENCE_QUESTIONS = "ds_ref_questions";

    public static void loadDatasets(ActionServlet servlet,
            HttpServletRequest request) throws Exception {
        WdkModelBean wdkModel = ActionUtility.getWdkModel(servlet);

        List<RecordBean> questionRefs = new ArrayList<RecordBean>();

        // if xml data source exists, bypass the process
        if (!GetDatasetAction.hasXmlDataset(wdkModel)) {

            // load the recordClass based data sources
            UserBean user = ActionUtility.getUser(servlet, request);
            String questionName = request.getParameter("questionFullName");
            QuestionBean question;
            if (questionName == null) {
                question = (QuestionBean) request.getAttribute(CConstants.WDK_QUESTION_KEY);
                questionName = question.getFullName();
            } else {
                question = wdkModel.getQuestion(questionName);
            }

            // get the data source question
            QuestionBean dsQuestion = wdkModel.getQuestion(GetDatasetAction.DATA_SOURCE_BY_QUESTION);
            Map<String, String> params = new LinkedHashMap<String, String>();
            params.put(PARAM_QUESTION, questionName);
            AnswerValueBean answerValue = dsQuestion.makeAnswerValue(user,
                    params, true, 0);

            // find all referenced attributes and tables;
            Iterator<RecordBean> dsRecords = answerValue.getRecords();
            while (dsRecords.hasNext()) {
                RecordBean dsRecord = dsRecords.next();
                TableValue tableValue = dsRecord.getTables().get(
                        TABLE_REFERENCE);
                for (Map<String, AttributeValue> row : tableValue) {
                    String targetType = row.get("target_type").toString();
                    String targetName = row.get("target_name").toString();
                    if (targetType.equals(TYPE_QUESTION)
                            && targetName.equals(questionName)) {
                        questionRefs.add(dsRecord);
                        break;
                    }
                }
            }
        }

        request.setAttribute(ATTR_REFERENCE_QUESTIONS, questionRefs);
    }

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        ActionForward forward = super.execute(mapping, form, request, response);
        loadDatasets(servlet, request);

        // run execute from parent
        return forward;
    }

}
