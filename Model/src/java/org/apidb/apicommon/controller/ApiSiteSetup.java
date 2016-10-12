package org.apidb.apicommon.controller;

import static org.apidb.apicommon.model.TranscriptUtil.isTranscriptRecordClass;

import org.apache.log4j.Logger;
import org.apidb.apicommon.model.filter.GeneBooleanFilter;
import org.gusdb.fgputil.events.Event;
import org.gusdb.fgputil.events.EventListener;
import org.gusdb.fgputil.events.Events;
import org.gusdb.wdk.events.StepRevisedEvent;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.filter.FilterOption;
import org.gusdb.wdk.model.query.BooleanQuery;
import org.gusdb.wdk.model.user.Step;
import org.json.JSONObject;

public class ApiSiteSetup {

  private static final Logger LOG = Logger.getLogger(ApiSiteSetup.class);
  
  /**
   * Initialize any parts of the ApiCommon web application not handled by normal
   * WDK initialization.
   * 
   * @param wdkModel initialized WDK model
   */
  public static void initialize(WdkModel wdkModel) {
    addTranscriptBooleanReviseListener();
  }

  private static void addTranscriptBooleanReviseListener() {
    Events.subscribe(new EventListener() {
      @Override public void eventTriggered(Event event) throws Exception {
        StepRevisedEvent reviseEvent = (StepRevisedEvent)event;
        Step revisedStep = reviseEvent.getRevisedStep();
        if (!revisedStep.isBoolean() || !isTranscriptRecordClass(revisedStep.getRecordClass())) {
          // only edit transcript boolean steps
          return;
        }
        // transcript boolean step was revised; make sure it was an operator change
        String newOperator = getOperator(revisedStep);
        String oldOperator = getOperator(reviseEvent.getPreviousVersion());
        if (newOperator.equalsIgnoreCase(oldOperator)) {
          // only modify default filter value if operator changed
          return;
        }
        // reset GeneBooleanFilter to default for new value
        FilterOption geneBooleanFilter = revisedStep.getFilterOptions()
            .getFilterOption(GeneBooleanFilter.GENE_BOOLEAN_FILTER_ARRAY_KEY);
        if (geneBooleanFilter == null) {
          throw new WdkModelException("Found transcript boolean step " +
              revisedStep.getStepId() + " without GeneBooleanFilter.");
        }
        if (!geneBooleanFilter.isSetToDefaultValue(revisedStep)) {
          JSONObject newValue = GeneBooleanFilter.getDefaultValue(newOperator);
          LOG.info("Resetting gene boolean filter on step " + revisedStep.getStepId() +
              " to default value: " + newValue.toString(2));
          revisedStep.addFilterOption(GeneBooleanFilter.GENE_BOOLEAN_FILTER_ARRAY_KEY, newValue);
          revisedStep.saveParamFilters();
        }
      }

      private String getOperator(Step revisedStep) throws WdkModelException {
        String operator = revisedStep.getParamValues().get(BooleanQuery.OPERATOR_PARAM);
        if (operator == null) {
          throw new WdkModelException("Found transcript boolen step " +
              revisedStep.getStepId() + " without " + BooleanQuery.OPERATOR_PARAM + " parameter.");
        }
        return operator;
      }
    }, StepRevisedEvent.class);
  }

}
