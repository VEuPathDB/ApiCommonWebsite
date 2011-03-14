package org.apidb.apicommon.controller.wizard;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionServlet;
import org.gusdb.wdk.controller.CConstants;
import org.gusdb.wdk.controller.action.ActionUtility;
import org.gusdb.wdk.controller.action.QuestionForm;
import org.gusdb.wdk.controller.action.ShowQuestionAction;
import org.gusdb.wdk.controller.action.WizardForm;
import org.gusdb.wdk.controller.wizard.StageHandler;
import org.gusdb.wdk.controller.wizard.StageHandlerUtility;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.jspwrap.AnswerParamBean;
import org.gusdb.wdk.model.jspwrap.AnswerValueBean;
import org.gusdb.wdk.model.jspwrap.ParamBean;
import org.gusdb.wdk.model.jspwrap.QuestionBean;
import org.gusdb.wdk.model.jspwrap.StepBean;
import org.gusdb.wdk.model.jspwrap.StrategyBean;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;
import org.json.JSONException;

public class ProcessOrthologStageHandler implements StageHandler {

    private static final Logger logger = Logger
            .getLogger(ProcessOrthologStageHandler.class);

    public Map<String, Object> execute(ActionServlet servlet,
            HttpServletRequest request, HttpServletResponse response,
            WizardForm wizardForm) throws Exception {
        logger.debug("Entering OrthologStageHandler....");

        // determine where to add the ortholog. If the current step is the root
        // step of a strategy, then we add it to the end; otherwise, we insert
        // the ortholog after current step.
        //
        // The above 2 use cases can be simplified into the following:
        // 1) create a ortholog transform using the current step as input
        // 2) revise the current step by replacing the current step with the new
        // ortholog step.
        StepBean currentStep = StageHandlerUtility.getCurrentStep(request);

        // get a span logic question
        WdkModelBean wdkModel = ActionUtility.getWdkModel(servlet);
        String questionName = request
                .getParameter(CConstants.QUESTION_FULLNAME_PARAM);
        QuestionBean question = wdkModel.getQuestion(questionName);
        AnswerParamBean answerParam = null;
        for(ParamBean param : question.getParams()) {
            if (param instanceof AnswerParamBean) {
                answerParam = (AnswerParamBean) param;
                break;
            }
        }

        // set the action to revise, and set the current step id as the input id
        // of the ortholog query.
        String stepId = Integer.toString(currentStep.getStepId());
        wizardForm.setAction(WizardForm.ACTION_REVISE);
        wizardForm.setValue(answerParam.getName(), stepId);

        Map<String, Object> attributes = new HashMap<String, Object>();
        return attributes;
    }
}
