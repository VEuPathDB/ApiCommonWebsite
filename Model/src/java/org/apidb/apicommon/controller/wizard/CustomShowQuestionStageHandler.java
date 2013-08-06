package org.apidb.apicommon.controller.wizard;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionServlet;
import org.apidb.apicommon.controller.action.CustomShowQuestionAction;
import org.gusdb.wdk.controller.form.WizardForm;
import org.gusdb.wdk.controller.wizard.ShowQuestionStageHandler;

public class CustomShowQuestionStageHandler extends ShowQuestionStageHandler {

    private static final Logger logger = Logger.getLogger(CustomShowQuestionStageHandler.class);

    /* (non-Javadoc)
     * @see org.gusdb.wdk.controller.wizard.ShowQuestionStageHandler#execute(org.apache.struts.action.ActionServlet, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.gusdb.wdk.controller.action.WizardForm)
     */
    @Override
    public Map<String, Object> execute(ActionServlet servlet,
            HttpServletRequest request, HttpServletResponse response,
            WizardForm wizardForm) throws Exception {
        logger.debug("Entering CustomShowQuestionStageHandler...");

        // load data sources
        CustomShowQuestionAction.loadReferences(servlet, request);
        
        // call execute() from parent;
        return super.execute(servlet, request, response, wizardForm);
    }

    
}
