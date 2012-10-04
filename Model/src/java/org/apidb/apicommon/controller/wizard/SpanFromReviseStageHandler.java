package org.apidb.apicommon.controller.wizard;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionServlet;
import org.gusdb.wdk.controller.form.WizardForm;
import org.gusdb.wdk.controller.wizard.StageHandlerUtility;
import org.gusdb.wdk.model.jspwrap.StepBean;

public class SpanFromReviseStageHandler extends ShowSpanStageHandler {

    @Override
    protected StepBean getChildStep(ActionServlet servlet,
            HttpServletRequest request, HttpServletResponse response,
            WizardForm wizardForm) throws Exception {
        StepBean currentStep = StageHandlerUtility.getCurrentStep(request);
        return currentStep.getChildStep();
    }

}
