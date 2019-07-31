package org.apidb.apicommon.model.report;

import org.gusdb.fgputil.validation.ValidObjectFactory.RunnableObj;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.answer.factory.AnswerValueFactory;
import org.gusdb.wdk.model.answer.spec.AnswerSpec;
import org.gusdb.wdk.model.answer.spec.AnswerSpecBuilder;
import org.gusdb.wdk.model.answer.spec.FilterOption;
import org.gusdb.wdk.model.answer.spec.FilterOptionList;
import org.gusdb.wdk.model.answer.spec.FilterOptionList.FilterOptionListBuilder;
import org.gusdb.wdk.model.record.attribute.AttributeField;
import org.gusdb.wdk.model.toolbundle.ColumnReporterInstance;
import org.gusdb.wdk.model.toolbundle.ColumnToolConfig;
import org.gusdb.wdk.model.toolbundle.reporter.MultiTypeColumnReporter;
import org.gusdb.wdk.service.request.exception.DataValidationException;
import org.gusdb.wdk.service.service.AnswerService;
import org.json.JSONObject;
import org.apidb.apicommon.model.filter.RepresentativeTranscriptFilter;

public class GeneMultiTypeColumnReporter extends MultiTypeColumnReporter {

  @Override
  public ColumnReporterInstance makeInstance(AnswerValue answerValue, AttributeField field,
      ColumnToolConfig config) throws WdkModelException {
    return super.makeInstance(applyRepresentativeTranscriptFilter(answerValue), field, config);
  }

  /**
   * get an answer value that applies the view-only filter that reduces the result to one
   * transcript per gene
   */
  private AnswerValue applyRepresentativeTranscriptFilter(AnswerValue originalAnswer) throws WdkModelException {
    AnswerSpec originalAnswerSpec = originalAnswer.getAnswerSpec();
    AnswerSpecBuilder specBuilder = AnswerSpec.builder(originalAnswerSpec);
    FilterOptionListBuilder viewFiltersBuilder = FilterOptionList.builder()
      .addAllFilters(originalAnswerSpec.getViewFilterOptions())
      .addFilterOption(FilterOption.builder()
        .setFilterName(RepresentativeTranscriptFilter.FILTER_NAME)
        .setValue(new JSONObject())
        .setDisabled(false));
    specBuilder.setViewFilterOptions(viewFiltersBuilder);

    try {
      RunnableObj<AnswerSpec> runnableSpec = specBuilder.buildRunnable(originalAnswer.getUser(),
          AnswerService.loadContainer(specBuilder, originalAnswer.getWdkModel(), originalAnswer.getUser()));
      return AnswerValueFactory.makeAnswer(originalAnswer, runnableSpec);
    }
    catch (DataValidationException e) {
      throw new WdkModelException(e);
    }
  }
}
