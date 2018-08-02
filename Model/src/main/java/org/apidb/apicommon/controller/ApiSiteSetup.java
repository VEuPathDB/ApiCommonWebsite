package org.apidb.apicommon.controller;

import static org.apidb.apicommon.model.TranscriptUtil.isTranscriptRecordClass;

import java.util.Map;

import org.apache.log4j.Logger;
import org.apidb.apicommon.model.comment.CommentFactory;
import org.apidb.apicommon.model.filter.GeneBooleanFilter;
import org.gusdb.fgputil.events.Event;
import org.gusdb.fgputil.events.EventListener;
import org.gusdb.fgputil.events.Events;
import org.gusdb.wdk.events.StepRevisedEvent;
import org.gusdb.wdk.events.UserProfileUpdateEvent;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.answer.spec.FilterOption;
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
    // add transcript boolean revise event listener
    Events.subscribe(TX_BOOLEAN_REVISE_LISTENER, StepRevisedEvent.class);
    // add user profile update event listener
    Events.subscribe(USER_PROFILE_UPDATE_LISTENER, UserProfileUpdateEvent.class);
  }

  /**
   * This code updates the value of the transcript boolean filter if the boolean operator (e.g. intersect,
   * union) is revised.  It will set the value to the default for the new operator.
   */
  private static final EventListener TX_BOOLEAN_REVISE_LISTENER = new EventListener() {

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
      String operator = revisedStep.getQueryInstanceSpec().get(BooleanQuery.OPERATOR_PARAM);
      if (operator == null) {
        throw new WdkModelException("Found transcript boolen step " +
            revisedStep.getStepId() + " without " + BooleanQuery.OPERATOR_PARAM + " parameter.");
      }
      return operator;
    }
  };

  /**
   * This code replaces a long-standing DB trigger that was used to update comment search text if the user
   * changed their profile information.  It must collect comment text for any comments owned by the revised
   * user and update their cached search text in the DB.
   */
  private static final EventListener USER_PROFILE_UPDATE_LISTENER = new EventListener() {
    @Override public void eventTriggered(Event event) throws Exception {
      UserProfileUpdateEvent updateEvent = (UserProfileUpdateEvent)event;

      // check to see if any of the property text fields changed
      Map<String,String> userProps = updateEvent.getNewProfile().getProperties();
      Map<String,String> oldProfileProps = updateEvent.getOldProfile().getProperties();
      boolean commentSearchTextUpdateRequired = false;
      for (String key : oldProfileProps.keySet()) {
        if (!oldProfileProps.get(key).equals(userProps.get(key))) {
          commentSearchTextUpdateRequired = true;
        }
      }

      // if none changed, no update needed
      if (!commentSearchTextUpdateRequired) return;

      // need to write updated text to comment search field
      CommentFactory commentFactory = CommentFactoryManager.getCommentFactory(updateEvent.getWdkModel().getProjectId());
      commentFactory.updateCommentUser(updateEvent.getNewProfile());
    }
  };
}
