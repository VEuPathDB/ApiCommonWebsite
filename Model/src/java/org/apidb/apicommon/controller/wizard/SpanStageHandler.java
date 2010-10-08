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
import org.gusdb.wdk.controller.action.WizardAction;
import org.gusdb.wdk.controller.action.WizardForm;
import org.gusdb.wdk.controller.wizard.StageHandler;
import org.gusdb.wdk.model.jspwrap.QuestionBean;
import org.gusdb.wdk.model.jspwrap.StepBean;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;

public abstract class SpanStageHandler implements StageHandler {

    private static final String ATTR_IMPORT_STEP = "importStep";
    private static final String ATTR_ALLOW_CHOOSE_OUTPUT = "allowChooseOutput";

    private static final String SPAN_QUESTION = CConstants.WDK_QUESTION_KEY;

    private static final Logger logger = Logger.getLogger(SpanFromQuestionStageHandler.class);

    protected abstract StepBean getImportedStep(ActionServlet servlet,
            HttpServletRequest request, HttpServletResponse response,
            WizardForm wizardForm) throws Exception;

    public Map<String, Object> execute(ActionServlet servlet,
            HttpServletRequest request, HttpServletResponse response,
            WizardForm wizardForm) throws Exception {
        logger.debug("Entering SpanFromQuestionStageHandler....");

        // get imported step
        StepBean importStep = getImportedStep(servlet, request, response,
                wizardForm);

        // get a span logic question
        WdkModelBean wdkModel = ActionUtility.getWdkModel(servlet);
        String spanQuestionName = ProcessSpanStageHandler.getSpanQuestion(importStep.getType());
        QuestionBean spanQuestion = wdkModel.getQuestion(spanQuestionName);

        // initialize the wizardForm so that it has the param information
        QuestionForm questionForm = new QuestionForm();
        questionForm.setQuestion(spanQuestion);
        questionForm.setServlet(servlet);
        ShowQuestionAction.prepareQuestionForm(spanQuestion, servlet, request,
                questionForm);
        wizardForm.copyFrom(questionForm);

        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(ATTR_IMPORT_STEP, importStep);
        attributes.put(SPAN_QUESTION, spanQuestion);

        // determine the input step
        String action = wizardForm.getAction();
        StepBean currentStep = (StepBean) request.getAttribute(WizardAction.ATTR_STEP);
        StepBean inputStep;
        if (action.equals(WizardForm.ACTION_ADD)) {
            // add, the current step is the last step of a strategy or a
            // sub-strategy, use it as the input;
            inputStep = currentStep;
        } else { // revise or insert,
            // the current step is always the lower step in the graph, no
            // matter whether it's a boolean, or a combined step. Use the
            // previous step as the input.
            inputStep = currentStep.getPreviousStep();
        }

        // if the current has any parent, disable the output choice option
        boolean chooseOutput = (currentStep.getParentStep() == null && currentStep.getNextStep() == null);
        attributes.put(ATTR_ALLOW_CHOOSE_OUTPUT, chooseOutput);

        // also set the step ids as the default of the the input params
        wizardForm.setValue("span_a", inputStep.getStepId());
        wizardForm.setValue("span_b", importStep.getStepId());

        logger.debug("Leaving SpanFromQuestionStageHandler....");
        return attributes;
    }
}
