package org.apidb.apicommon.controller.wizard;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionServlet;
import org.gusdb.wdk.controller.CConstants;
import org.gusdb.wdk.controller.action.ActionUtility;
import org.gusdb.wdk.controller.action.WizardForm;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.jspwrap.ParamBean;
import org.gusdb.wdk.model.jspwrap.QuestionBean;
import org.gusdb.wdk.model.jspwrap.StepBean;
import org.gusdb.wdk.model.jspwrap.UserBean;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;

public class SpanFromQuestionStageHandler extends SpanStageHandler {

    private static final Logger logger = Logger.getLogger(SpanFromQuestionStageHandler.class);

    public StepBean getImportedStep(ActionServlet servlet,
            HttpServletRequest request, HttpServletResponse response,
            WizardForm wizardForm) throws Exception {
        logger.debug("Entering SpanFromQuestionStageHandler....");

        // create a new step from question
        String questionName = request.getParameter(CConstants.QUESTION_FULLNAME_PARAM);
        if (questionName == null || questionName.length() == 0)
            throw new WdkUserException("Required "
                    + CConstants.QUESTION_FULLNAME_PARAM + " is missing.");

        WdkModelBean wdkModel = ActionUtility.getWdkModel(servlet);
        QuestionBean question = wdkModel.getQuestion(questionName);

        Map<String, String> params = new HashMap<String, String>();
        for (ParamBean param : question.getParams()) {
            String paramName = param.getName();
            Object value = wizardForm.getValueOrArray(paramName);
            params.put(paramName, (String) value);
        }

        // get the assigned weight
        String strWeight = request.getParameter(CConstants.WDK_ASSIGNED_WEIGHT_KEY);
        boolean hasWeight = (strWeight != null && strWeight.length() > 0);
        int weight = 0;
        if (hasWeight) {
            if (!strWeight.matches("[\\-\\+]?\\d+"))
                throw new WdkUserException("Invalid weight value: '"
                        + strWeight + "'. Only integers are allowed.");
            if (strWeight.length() > 9)
                throw new WdkUserException("Weight number is too big: "
                        + strWeight);
            weight = Integer.parseInt(strWeight);
        }

        // create a step from the input
        String filterName = request.getParameter("filter");

        UserBean user = ActionUtility.getUser(servlet, request);
        StepBean importedStep = user.createStep(question, params, filterName, false,
                true, weight);

        logger.debug("Leaving SpanFromQuestionStageHandler....");
        return importedStep;
    }

}
