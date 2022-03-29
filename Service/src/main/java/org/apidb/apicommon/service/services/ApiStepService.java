package org.apidb.apicommon.service.services;

import static org.apidb.apicommon.model.TranscriptUtil.isTranscriptRecordClass;

import java.util.Optional;

import jakarta.ws.rs.PathParam;

import org.apache.log4j.Logger;
import org.apidb.apicommon.model.filter.GeneBooleanFilter;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.answer.spec.FilterOption;
import org.gusdb.wdk.model.answer.spec.SimpleAnswerSpec;
import org.gusdb.wdk.model.query.BooleanOperator;
import org.gusdb.wdk.model.query.BooleanQuery;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.user.Step.StepBuilder;
import org.gusdb.wdk.model.user.StepContainer;
import org.gusdb.wdk.service.service.user.StepService;
import org.json.JSONObject;

public class ApiStepService extends StepService {

  private static final Logger LOG = Logger.getLogger(ApiStepService.class);

  public ApiStepService(@PathParam(USER_ID_PATH_PARAM) String uid) {
    super(uid);
  }

  /**
   * This code updates the value of the transcript boolean filter if the boolean
   * operator (e.g. intersect, union) of a transcript boolean step is revised.
   * It will set the value to the default for the new operator.
   * 
   * @param existingStep previous version of the step
   * @param replacementBuilder a replacement builder with client-requested changes applied
   * @throws WdkModelException 
   */
  @Override
  protected void applyAdditionalChanges(Step existingStep, StepBuilder replacementBuilder)
      throws WdkModelException {

    if (!existingStep.hasBooleanQuestion() || !isTranscriptRecordClass(existingStep.getRecordClass()
        .orElseThrow(() -> new WdkModelException("Passed existing step does not have a valid question.")))) {
      // only edit transcript boolean steps
      return;
    }

    // transcript boolean step was revised; make sure it was an operator change
    BooleanOperator newOperator = BooleanQuery.getOperator(replacementBuilder.getAnswerSpec().getQueryInstanceSpecBuilder(), Optional.of(replacementBuilder.getStepId()));
    BooleanOperator oldOperator = BooleanQuery.getOperator(existingStep.getAnswerSpec().getQueryInstanceSpec(), Optional.of(existingStep.getStepId()));
    if (newOperator.equals(oldOperator)) {
      // only modify default filter value if operator changed
      return;
    }

    // get the GeneBooleanFilter value in the new version
    FilterOption geneBooleanFilter = replacementBuilder.getAnswerSpec().getFilterOptions()
        .stream()
        .filter(option -> option.getFilterName().equals(GeneBooleanFilter.GENE_BOOLEAN_FILTER_ARRAY_KEY))
        .findAny()
        .orElseThrow(() -> new WdkModelException("Found transcript boolean step " +
            existingStep.getStepId() + " without GeneBooleanFilter."))
        .buildInvalid();

    // get a simple answer spec from our replacement builder
    SimpleAnswerSpec answerSpec = replacementBuilder.getAnswerSpec()
        .build(existingStep.getUser(), StepContainer.emptyContainer(), ValidationLevel.NONE)
        .toSimpleAnswerSpec();

    // only modify the step if filter value is not already set the default
    if (!geneBooleanFilter.isSetToDefaultValue(answerSpec)) {
      JSONObject newValue = GeneBooleanFilter.getDefaultValue(newOperator);
      LOG.info("Resetting gene boolean filter on step " + existingStep.getStepId() +
          " to default value: " + newValue.toString(2));
      replacementBuilder.getAnswerSpec().replaceFirstFilterOption(
          GeneBooleanFilter.GENE_BOOLEAN_FILTER_ARRAY_KEY,
          option -> option.setValue(newValue));
    }
  }
}
