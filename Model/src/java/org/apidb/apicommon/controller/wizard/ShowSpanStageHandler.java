package org.apidb.apicommon.controller.wizard;

import java.util.HashMap;
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
import org.gusdb.wdk.model.jspwrap.QuestionBean;
import org.gusdb.wdk.model.jspwrap.StepBean;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;

public abstract class ShowSpanStageHandler implements StageHandler {

    private static final String ATTR_IMPORT_STEP = "importStep";
    private static final String ATTR_ALLOW_CHOOSE_OUTPUT = "allowChooseOutput";

    private static final String SPAN_QUESTION = CConstants.WDK_QUESTION_KEY;

    private static final Logger logger = Logger.getLogger(SpanFromQuestionStageHandler.class);

    protected abstract StepBean getChildStep(ActionServlet servlet,
            HttpServletRequest request, HttpServletResponse response,
            WizardForm wizardForm) throws Exception;

    public Map<String, Object> execute(ActionServlet servlet,
            HttpServletRequest request, HttpServletResponse response,
            WizardForm wizardForm) throws Exception {
        logger.debug("Entering SpanFromQuestionStageHandler....");

        // get child step
        StepBean childStep = getChildStep(servlet, request, response,
                wizardForm);

        // get a span logic question
        WdkModelBean wdkModel = ActionUtility.getWdkModel(servlet);
        String spanQuestionName = ProcessSpanStageHandler.getSpanQuestion(childStep.getType());
        QuestionBean spanQuestion = wdkModel.getQuestion(spanQuestionName);

        // initialize the wizardForm so that it has the param information
        QuestionForm questionForm = new QuestionForm();
        questionForm.setQuestion(spanQuestion);
        questionForm.setServlet(servlet);
        ShowQuestionAction.prepareQuestionForm(spanQuestion, servlet, request,
                questionForm);
        wizardForm.copyFrom(questionForm);

        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(ATTR_IMPORT_STEP, childStep);
        attributes.put(SPAN_QUESTION, spanQuestion);

        // determine the previous step
        String action = wizardForm.getAction();
        StepBean previousStep = StageHandlerUtility.getPreviousStep(servlet, request, wizardForm);
        boolean chooseOutput = true;
        if (!action.equals(WizardForm.ACTION_ADD)) {
            StepBean currentStep = StageHandlerUtility.getCurrentStep(request);

            // now get the new next step after the newly added span step, and
            // determine if the next step allows you to change type
            StepBean nextStep = action.equals(WizardForm.ACTION_INSERT) ? currentStep
                    : currentStep.getParentOrNextStep();
            String childType = childStep.getType();
            String previousType = previousStep.getType();
            chooseOutput = (nextStep == null || childType.equals(previousType));
        }

        // if the current has any parent, disable the output choice option
        attributes.put(ATTR_ALLOW_CHOOSE_OUTPUT, chooseOutput);

        // also set the step ids as the default of the the input params
        wizardForm.setValue("span_a", previousStep.getStepId());
        wizardForm.setValue("span_b", childStep.getStepId());

        logger.debug("Leaving SpanFromQuestionStageHandler....");
        return attributes;
    }

}
