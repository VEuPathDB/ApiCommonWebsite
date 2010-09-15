package org.apidb.apicommon.controller.wizard;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionServlet;
import org.apidb.apicomplexa.wsfplugin.spanlogic.SpanCompositionPlugin;
import org.gusdb.wdk.controller.action.ActionUtility;
import org.gusdb.wdk.controller.action.WizardAction;
import org.gusdb.wdk.controller.action.WizardForm;
import org.gusdb.wdk.controller.wizard.QuestionStageHandler;
import org.gusdb.wdk.controller.wizard.StageHandler;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.jspwrap.StepBean;
import org.gusdb.wdk.model.jspwrap.UserBean;

public class ProcessSpanStageHandler implements StageHandler {

    private static final String ATTR_QUESTION_NAME = "questionFullName";

    private static final Logger logger = Logger.getLogger(ProcessSpanStageHandler.class);

    public Map<String, Object> execute(ActionServlet servlet, HttpServletRequest request,
            HttpServletResponse response, WizardForm wizardForm)
            throws Exception {
        logger.debug("Entering ProcessSpanStageHandler....");

        String output = (String) wizardForm.getValueOrArray(SpanCompositionPlugin.PARAM_OUTPUT);

        StepBean step;
        if (output.equals(SpanCompositionPlugin.PARAM_VALUE_OUTPUT_A)) {
            // select step a as output
            step = (StepBean) request.getAttribute(WizardAction.ATTR_STEP);
        } else {
            // select step b as output
            String strStepId = request.getParameter(SpanCompositionPlugin.PARAM_SPAN_PREFIX
                    + "a");
            int stepId = Integer.valueOf(strStepId);

            UserBean user = ActionUtility.getUser(servlet, request);
            step = user.getStep(stepId);
        }
        String type = step.getType();
        String questionName = null;
        if (type.equals("GeneRecordClasses.GeneRecordClass")) {
            questionName = "SpanQuestions.GenesBySpanLogic";
        } else if (type.equals("OrfRecordClasses.OrfRecordClass")) {
            questionName = "SpanQuestions.OrfsBySpanLogic";
        } else if (type.equals("IsolateRecordClasses.IsolateRecordClass")) {
            questionName = "SpanQuestions.IsolatesBySpanLogic";
        } else if (type.equals("SnpRecordClasses.SnpRecordClass")) {
            questionName = "SpanQuestions.SnpsBySpanLogic";
        } else if (type.equals("DynSpanRecordClasses.DynSpanRecordClass")) {
            questionName = "SpanQuestions.DynSpansBySpanLogic";
        } else {
            throw new WdkUserException("The step #" + step.getStepId()
                    + " of type " + type
                    + " is not supported in Span Logic operation.");
        }

        Map<String, Object> results = new HashMap<String, Object>();
        results.put(ATTR_QUESTION_NAME, questionName);

        logger.debug("Leaving ProcessSpanStageHandler....");
        return results;
    }

}
