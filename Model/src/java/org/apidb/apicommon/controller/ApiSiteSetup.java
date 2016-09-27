package org.apidb.apicommon.controller;

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
        Step revisedStep = ((StepRevisedEvent)event).getRevisedStep();
        if (revisedStep.isBoolean() && revisedStep.getRecordClass().getFullName()
            .equals("TranscriptRecordClasses.TranscriptRecordClass")) {
          // transcript boolean step was revised; reset GeneBooleanFilter to default for new value
          FilterOption geneBooleanFilter = revisedStep.getFilterOptions()
              .getFilterOption(GeneBooleanFilter.GENE_BOOLEAN_FILTER_ARRAY_KEY);
          if (geneBooleanFilter == null) {
            throw new WdkModelException("Found transcript boolean step " +
                revisedStep.getStepId() + " without GeneBooleanFilter.");
          }
          String operator = revisedStep.getParamValues().get(BooleanQuery.OPERATOR_PARAM);
          if (operator == null) {
            throw new WdkModelException("Found transcript boolen step " +
                revisedStep.getStepId() + " without " + BooleanQuery.OPERATOR_PARAM + " parameter.");
          }
          if (!geneBooleanFilter.isSetToDefaultValue(revisedStep)) {
            JSONObject newValue = GeneBooleanFilter.getDefaultValue(operator);
            LOG.info("Resetting gene boolean filter on step " + revisedStep.getStepId() +
                " to default value: " + newValue.toString(2));
            revisedStep.addFilterOption(GeneBooleanFilter.GENE_BOOLEAN_FILTER_ARRAY_KEY, newValue);
            revisedStep.saveParamFilters();
          }
        }
      }
    }, StepRevisedEvent.class);
  }

}
