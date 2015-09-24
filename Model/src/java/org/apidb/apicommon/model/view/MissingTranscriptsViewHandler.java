package org.apidb.apicommon.model.view;

import java.util.HashMap;
import java.util.Map;

import org.gusdb.wdk.controller.summary.ResultTablePaging;
import org.gusdb.wdk.controller.summary.SummaryTableUpdateProcessor;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.AnswerValueAttributes;
import org.gusdb.wdk.model.answer.SummaryViewHandler;
import org.gusdb.wdk.model.jspwrap.AnswerValueBean;
import org.gusdb.wdk.model.jspwrap.StepBean;
import org.gusdb.wdk.model.jspwrap.UserBean;
import org.gusdb.wdk.model.record.attribute.AttributeField;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.user.User;

public class MissingTranscriptsViewHandler implements SummaryViewHandler {

  private static final String USER_PREFERENCE_SUFFIX = "_missingTranscriptsView";
  private static final String MISSING_TRANSCRIPTS_STEP = "missingTranscriptsStep";
  private static final String TRANSCRIPT_ID_FIELD = "source_id";

	@Override
	public Map<String, Object> process(Step step, Map<String, String[]> parameters, User user, WdkModel wdkModel)
			throws WdkModelException, WdkUserException {

	  // get answer from Transcripts step
	  UserBean userBean = new UserBean(user);
	  StepBean stepBean = new StepBean(userBean, step);
	  AnswerValueBean answer = stepBean.getAnswerValue();

	  // get new step, to make result view from
	  // use original step id, so that state is in this view is associated with it
	  Step newStep = new Step(wdkModel.getStepFactory(), user, step.getStepId()); 
	  newStep.setInMemoryOnly(true);
		
	  newStep.setQuestionName("InternalQuestions.GenesByMissingTranscriptsTransform");
	  Map<String, String> paramValues = new HashMap<String, String>();
	  paramValues.put("gene_result", "" + step.getStepId());
	  newStep.setParamValues(paramValues);
		
	  // override attributes so they are remembered in the step using the suffix
	  AnswerValueAttributes attributes = answer.getAnswerValue().getAttributes();
	  //	  AttributeField pkField = stepBean.getQuestion().getRecordClass()
	  //	    .getPrimaryKeyAttribute().getPrimaryKeyAttributeField();
    AttributeField pkField = step.getQuestion().getRecordClass().getAttributeFieldMap().get(TRANSCRIPT_ID_FIELD);
	  Map<String, AttributeField> summaryFields = AnswerValueAttributes
	    .buildSummaryAttributeFieldMap(user, step.getQuestion(), USER_PREFERENCE_SUFFIX, pkField);
	  attributes.overrideSummaryAttributeFieldMap(summaryFields);

	  StepBean newStepBean = new StepBean(userBean, newStep);

	  Map<String, Object> model = ResultTablePaging.processPaging(parameters, newStepBean.getQuestion(), userBean, newStepBean.getAnswerValue());

	  // pass the new step to the JSP to be rendered instead of the normal step
	  model.put(MISSING_TRANSCRIPTS_STEP,  newStepBean);

	  return model;
	}

	@Override
	public String processUpdate(Step step, Map<String, String[]> parameters, User user, WdkModel wdkModel)
			throws WdkModelException, WdkUserException {
		return SummaryTableUpdateProcessor.processUpdates(step, parameters, user, wdkModel, USER_PREFERENCE_SUFFIX);

	}

}
