package org.apidb.apicommon.controller.wizard;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionServlet;
import org.gusdb.wdk.controller.actionutil.ActionUtility;
import org.gusdb.wdk.controller.form.WizardForm;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.jspwrap.StepBean;
import org.gusdb.wdk.model.jspwrap.StrategyBean;
import org.gusdb.wdk.model.jspwrap.UserBean;

public class SpanFromStrategyStageHandler extends ShowSpanStageHandler {

  private static final String PARAM_STRATEGY = "importStrategy";

  private static final String PARAM_IMPORT_STRATEGY = "importStrategy";

  private static final Logger logger = Logger.getLogger(SpanFromQuestionStageHandler.class);

  @Override
  public StepBean getChildStep(ActionServlet servlet, HttpServletRequest request,
      HttpServletResponse response, WizardForm wizardForm) throws Exception {
    logger.debug("Entering SpanFromQuestionStageHandler....");

    // load strategy
    String strImportStrategyId = request.getParameter(PARAM_IMPORT_STRATEGY);

    if (strImportStrategyId == null || strImportStrategyId.length() == 0)
      throw new WdkUserException("required " + PARAM_IMPORT_STRATEGY + " is missing.");

    UserBean user = ActionUtility.getUser(servlet, request);

    int importStrategyId = Integer.valueOf(strImportStrategyId);
    StrategyBean importStrategy = user.getStrategy(importStrategyId);
    StepBean step = importStrategy.getLatestStep();
    StepBean childStep = step.deepClone(null, new HashMap<Integer, Integer>());
    childStep.setIsCollapsible(true);
    childStep.setCollapsedName("Copy of " + importStrategy.getName());
    childStep.update(false);

    logger.debug("Leaving SpanFromQuestionStageHandler....");
    return childStep;
  }
}
