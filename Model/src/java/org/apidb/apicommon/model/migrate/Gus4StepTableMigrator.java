package org.apidb.apicommon.model.migrate;

import static org.apidb.apicommon.model.TranscriptUtil.isTranscriptRecordClass;
import static org.apidb.apicommon.model.filter.FilterValueArrayUtil.getFilterValueArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.apidb.apicommon.model.TranscriptUtil;
import org.apidb.apicommon.model.filter.GeneBooleanFilter;
import org.apidb.apicommon.model.filter.MatchedTranscriptFilter;
import org.eupathdb.common.fix.NonApiGus4StepMigrationPlugin;
import org.eupathdb.common.fix.UpdatedStepWriter;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.MapBuilder;
import org.gusdb.fgputil.Tuples.ThreeTuple;
import org.gusdb.fgputil.functional.FunctionalInterfaces.Function;
import org.gusdb.fgputil.functional.Functions;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.filter.FilterOption;
import org.gusdb.wdk.model.fix.table.TableRowInterfaces.RowResult;
import org.gusdb.wdk.model.fix.table.TableRowInterfaces.TableRowUpdaterPlugin;
import org.gusdb.wdk.model.fix.table.TableRowInterfaces.TableRowWriter;
import org.gusdb.wdk.model.fix.table.TableRowUpdater;
import org.gusdb.wdk.model.fix.table.steps.StepData;
import org.gusdb.wdk.model.fix.table.steps.StepDataFactory;
import org.gusdb.wdk.model.fix.table.steps.StepDataTestWriter;
import org.gusdb.wdk.model.fix.table.steps.StepDataWriter;
import org.gusdb.wdk.model.fix.table.steps.StepQuestionUpdater;
import org.gusdb.wdk.model.query.BooleanQuery;
import org.gusdb.wdk.model.question.Question;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.model.user.Step;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Performs operations on steps required for EuPathDB GUS4/alt-splice release
 * 
 * 1. Change filters: {} to [] when found
 * 2. Add matched transcript filter to all leaf transcript steps
 * 3. Add boolean filter to all boolean transcript steps
 * 4. Remove use_boolean_filter param when found
 * 5. For parameters that are filterParams: convert value "Unknown" to null
 * 
 * From Trello:
 * 1. Add matched transcript filter to all leaf transcript steps
 * 2. Add boolean filter to all boolean transcript steps
 * 3. Change filters: {} to [] when found
 * 4. Remove use_boolean_filter param when found
 * 5. For parameters that are filterParams: convert value "Unknown" to null
 * 
 * @author rdoherty
 */
public class Gus4StepTableMigrator implements TableRowUpdaterPlugin<StepData> {

  private static final Logger LOG = Logger.getLogger(Gus4StepTableMigrator.class);

  private static final boolean LOG_INVALID_STEPS = false;
  private static final boolean LOG_PARAM_FILTER_DIFFS = false;
  private static final boolean LOG_LOADED_QUESTION_MAPPING = false;

  private static final AtomicInteger INVALID_STEP_COUNT_QUESTION = new AtomicInteger(0);
  private static final AtomicInteger INVALID_STEP_COUNT_PARAMS = new AtomicInteger(0);

  private static enum UpdateType {
    paramFilterRecordClasses,
    questionNameUpdate,
    updateParams,
    fixFilters,
    addViewFilters,
    removeOldDisplayParamProps,
    useBoolFilter,
    matchedTxFilter,
    geneBoolFilter,
    fixFilterParamValues,
    removeTxFilterFromNonTxSteps
  }

  private static final Map<UpdateType, AtomicInteger> UPDATE_TYPE_COUNTS =
      Functions.mapKeys(Arrays.asList(UpdateType.values()), new Function<UpdateType, AtomicInteger>() {
        @Override public AtomicInteger apply(UpdateType obj) { return new AtomicInteger(0); }});

  private WdkModel _wdkModel;
  private StepQuestionUpdater _qNameUpdater;
  private boolean _useTestWriter;

  @Override
  public void configure(WdkModel wdkModel, List<String> args) throws IOException {
    if (args.size() < 1 || args.size() > 2 || (args.size() == 2 && !args.get(1).equals("test"))) {
      throw new IllegalArgumentException(
          "\n\nIncorrect arguments.  Plugin args: <question_map_file> [test]\n\n" +
          "question_map_file: file in the format from=to on each row mapping old question names to new question names\n\n" +
          "Using optional 'test' argument will:\n" +
          " 1. Not update actual step values in STEPS table\n" +
          " 2. Will instead write display_params changes and other info to <login_schema>.STEP_UPDATER_PLUGIN_TEST table\n" +
          " 3. Will (!!) (still) write modified step IDs to <login_schema>.WDK_UPDATED_STEPS table\n\n");
    }
    _wdkModel = wdkModel;
    _qNameUpdater = new StepQuestionUpdater(args.get(0), LOG_LOADED_QUESTION_MAPPING);
    _useTestWriter = (args.size() == 2);
  }

  @Override
  public TableRowUpdater<StepData> getTableRowUpdater(WdkModel wdkModel) {
    StepDataFactory factory = new StepDataFactory(false);
    return (_useTestWriter ?
        // use test writer and do not write modified step IDs to wdk_updated_steps
        new TableRowUpdater<StepData>(factory, getWriterList(new StepDataTestWriter()), this, wdkModel) :
        // otherwise use 'real' writer and write modified step IDs to wdk_updated_steps
        new TableRowUpdater<StepData>(factory, getWriterList(new StepDataWriter()), this, wdkModel));
  }

  private List<TableRowWriter<StepData>> getWriterList(StepDataWriter stepDataWriter) {
    return new ListBuilder<TableRowWriter<StepData>>()
        .add(stepDataWriter)
        .add(new UpdatedStepWriter())
        .toList();
  }

  @Override
  public void dumpStatistics() {
    LOG.info("Invalid Steps:");
    LOG.info("  " + INVALID_STEP_COUNT_QUESTION.get() + " steps still have invalid questions");
    LOG.info("  " + INVALID_STEP_COUNT_PARAMS.get() + " steps have invalid param names");
    LOG.info("   Note: param values are not validated and must be checked separately");
    LOG.info("Updated Steps:");
    for (UpdateType type : UpdateType.values()) {
      LOG.info("  " + UPDATE_TYPE_COUNTS.get(type).get() + " steps updated by '" + type.name() + "'");
    }
  }

  @Override
  public RowResult<StepData> processRecord(StepData step) throws WdkModelException {

    // this list will record the parts of processing that modify the step (can be >1)
    List<UpdateType> mods = new ArrayList<>();

    // 1. Replace strings in display_params based on a few old question names, then convert those names
    if (fixParamFilterRecordClasses(step)) mods.add(UpdateType.paramFilterRecordClasses);

    // 2. Use QuestionMapper to update question names
    if (_qNameUpdater.updateQuestionName(step)) mods.add(UpdateType.questionNameUpdate);
    
    // 3. If "params" prop not present then place entire paramFilters inside and write back
    if (NonApiGus4StepMigrationPlugin.updateParamsProperty(step)) mods.add(UpdateType.updateParams);

    // 4. Add "filters" property if not present and convert any found objects to filter array
    if (NonApiGus4StepMigrationPlugin.updateFiltersProperty(step, Step.KEY_FILTERS)) mods.add(UpdateType.fixFilters);
    if (NonApiGus4StepMigrationPlugin.updateFiltersProperty(step, Step.KEY_VIEW_FILTERS)) mods.add(UpdateType.addViewFilters);

    // 5. Remove use_boolean_filter param when found
    if (NonApiGus4StepMigrationPlugin.removeUseBooleanFilterParam(step)) mods.add(UpdateType.useBoolFilter);

    // 6. Some steps have both a params property and params as top-level properties in display_params; remove the latter
    if (NonApiGus4StepMigrationPlugin.removeOldDisplayParamProps(step)) mods.add(UpdateType.removeOldDisplayParamProps);

    // Look up some WDK model data needed by the remaining sections.  Doing it AFTER the above steps since
    //   question names will have been updated and we won't kick out as many invalid steps whose question
    //   names are missing from the current model.
    Question question;
    try {
      // use (possibly already modified) question name to look up question in the current model
      question = _wdkModel.getQuestion(step.getQuestionName());
    }
    catch (WdkModelException e) {
      int invalidStepsByQuestion = INVALID_STEP_COUNT_QUESTION.incrementAndGet();
      if (LOG_INVALID_STEPS)
        LOG.warn("Question name " + step.getQuestionName() + " does not appear in the WDK model (" +
            invalidStepsByQuestion + " total invalid steps by question).");
      return new RowResult<StepData>(step).setShouldWrite(!mods.isEmpty());
    }
    RecordClass recordClass = question.getRecordClass();
    boolean isBoolean = question.getQuery().isBoolean();
    boolean isLeaf = !question.getQuery().isCombined();

    // 7. Add matched transcript filter to all leaf transcript steps
    if (addMatchedTranscriptFilter(step, isLeaf, recordClass)) mods.add(UpdateType.matchedTxFilter);

    // 8. Add gene boolean filter to all boolean transcript steps
    if (addGeneBooleanFilter(step, isBoolean, recordClass)) mods.add(UpdateType.geneBoolFilter);

    // 9. Filter param format has changed a bit; update existing steps to comply
    if (NonApiGus4StepMigrationPlugin.fixFilterParamValues(step, question,
        LOG_INVALID_STEPS, INVALID_STEP_COUNT_PARAMS)) mods.add(UpdateType.fixFilterParamValues);

    // 10. Remove “matched_transcript_filter_array” filter from non-transcript steps
    if (removeTxFilterFromNonTxSteps(step, question)) mods.add(UpdateType.removeTxFilterFromNonTxSteps);

    // log modification and return result
    RowResult<StepData> result = new RowResult<StepData>(step).setShouldWrite(!mods.isEmpty());
    if (result.shouldWrite()) {
      LOG.info("Step " + result.getRow().getStepId() + " modified by " + FormatUtil.arrayToString(mods.toArray()));
      for (UpdateType type : mods) {
        UPDATE_TYPE_COUNTS.get(type).incrementAndGet();
      }
      if (LOG_PARAM_FILTER_DIFFS) {
        LOG.info("Incoming paramFilters: " + new JSONObject(step.getOrigParamFiltersString()).toString(2));
        LOG.info("Outgoing paramFilters: " + step.getParamFilters().toString(2));
      }
    }
    return result;
  }

  private static boolean addGeneBooleanFilter(StepData step, boolean isBoolean, RecordClass recordClass) throws WdkModelException, JSONException {
    if (!isBoolean) return false;
    if (!isTranscriptRecordClass(recordClass)) return false;
    // figure out default value based on boolean param
    JSONObject params = step.getParamFilters().getJSONObject(Step.KEY_PARAMS);
    boolean isWdkSetOperation = params.has(BooleanQuery.OPERATOR_PARAM);
    if (!isWdkSetOperation) {
      LOG.warn("Found boolean step (ID " + step.getStepId() + ") that does not have param " + BooleanQuery.OPERATOR_PARAM);
      return false;
    }
    JSONObject defaultValue = GeneBooleanFilter.getDefaultValue(params.getString(BooleanQuery.OPERATOR_PARAM));
    if (defaultValue == null) return false;
    return addFilterValueArray(step, GeneBooleanFilter.GENE_BOOLEAN_FILTER_ARRAY_KEY, defaultValue);
  }

  private static boolean addMatchedTranscriptFilter(StepData step, boolean isLeaf, RecordClass recordClass) {
    // requirements:
    //   transcript question
    //   leaf step
    //   non-basket
    if (!isLeaf) return false;
    if (!isTranscriptRecordClass(recordClass)) return false;
    if (step.getQuestionName().toLowerCase().contains("basket")) return false;
    // add filter with default value if not already present
    JSONObject defaultValue = getFilterValueArray("Y");
    return addFilterValueArray(step, MatchedTranscriptFilter.MATCHED_TRANSCRIPT_FILTER_ARRAY_KEY, defaultValue);
  }

  private static boolean addFilterValueArray(StepData step, String name, JSONObject value) {
    JSONArray filters = step.getParamFilters().getJSONArray(Step.KEY_FILTERS);
    for (int i = 0; i < filters.length(); i++) {
      JSONObject filterData = filters.getJSONObject(i);
      if (filterData.getString(FilterOption.KEY_NAME).equals(name)) {
        // filter already present; doesn't need to be added
        return false;
      }
    }
    // filter not present; must add to front of list
    if (filters.length() > 0) {
      // displace old filters; default should go at the front
      for (int i = filters.length() - 1; i > -1; i--) {
        filters.put(i + 1, filters.getJSONObject(i));
      }
    }
    // create filter object
    JSONObject filterObject = new JSONObject();
    filterObject.put(FilterOption.KEY_NAME, name);
    filterObject.put(FilterOption.KEY_VALUE, value);
    filterObject.put(FilterOption.KEY_DISABLED, false);
    filters.put(0, filterObject);
    return true;
  }

  /*
   * display_param replacements- tuples of "fromString", "toString", array of question names to apply to
   * 
   * Encapsulates the data values in the following SQL:
   * 
   * Steps table: display_params contains “GeneRecord”
   * 
   * UPDATE userlogins5.steps
   * SET display_params=replace(display_params, 'GeneRecordClass', 'TranscriptRecordClass')
   * WHERE question_name in ('GeneQuestions.GenesBySimilarity', 'GenomicSequenceQuestions.SequencesBySimilarity', 'InternalQuestions.GeneRecordClasses_GeneRecordClassBySnapshotBasket', 'InternalQuestions.boolean_question_GeneRecordClasses_GeneRecordClass');
   * 
   * Steps table: display_params contains “IsolateRecord”
   * 
   * UPDATE userlogins5.steps
   * SET display_params=replace(display_params, 'IsolateRecordClass', 'PopsetRecordClass')
   * WHERE question_name in ('IsolateQuestions.IsolatesBySimilarity', 'InternalQuestions.IsolateRecordClasses_IsolateRecordClassBySnapshotBasket', 'InternalQuestions.boolean_question_IsolateRecordClasses_IsolateRecordClass');
   */
  private static final List<ThreeTuple<String, String, List<String>>> DISPLAY_PARAM_REPLACEMENTS =
      new ListBuilder<ThreeTuple<String, String, List<String>>>()
      .add(new ThreeTuple<String, String, List<String>>(
          "GeneRecordClass", "TranscriptRecordClass", Arrays.asList(new String[] {
              "GeneQuestions.GenesBySimilarity",
              "GenomicSequenceQuestions.SequencesBySimilarity",
              "InternalQuestions.GeneRecordClasses_GeneRecordClassBySnapshotBasket",
              "InternalQuestions.boolean_question_GeneRecordClasses_GeneRecordClass",
              "GeneQuestions.GeneBySingleLocusTag",
              "InternalQuestions.TranscriptRecordClasses_TranscriptRecordClassBySnapshotBasket",
              "InternalQuestions.boolean_question_TranscriptRecordClasses_TranscriptRecordClass"
          })))
      .add(new ThreeTuple<String, String, List<String>>(
          "IsolateRecordClass", "PopsetRecordClass", Arrays.asList(new String[] {
              "IsolateQuestions.IsolatesBySimilarity",
              "InternalQuestions.IsolateRecordClasses_IsolateRecordClassBySnapshotBasket",
              "InternalQuestions.boolean_question_IsolateRecordClasses_IsolateRecordClass",
              "InternalQuestions.PopsetRecordClasses_PopsetRecordClassBySnapshotBasket",
              "InternalQuestions.boolean_question_PopsetRecordClasses_PopsetRecordClass"
          })))
      .toList();

  /*
   * question name replacements- tuples of "fromName", "toName"
   * 
   * Encapsulates the data values in the following SQL:
   * 
   * Steps table: question_name contains “GeneRecord” 
   * 
   * UPDATE userlogins5.steps
   * SET question_name='InternalQuestions.boolean_question_TranscriptRecordClasses_TranscriptRecordClass'
   * WHERE question_name = 'InternalQuestions.boolean_question_GeneRecordClasses_GeneRecordClass'
   * 
   * UPDATE userlogins5.steps
   * SET question_name='InternalQuestions.TranscriptRecordClasses_TranscriptRecordClassBySnapshotBasket'
   * WHERE question_name = 'InternalQuestions.GeneRecordClasses_GeneRecordClassBySnapshotBasket'
   * 
   * Steps table: question_name contains “IsolateRecord”
   * 
   * UPDATE userlogins5.steps
   * SET question_name='InternalQuestions.boolean_question_PopsetRecordClasses_PopsetRecordClass'
   * WHERE question_name = 'InternalQuestions.boolean_question_IsolateRecordClasses_IsolateRecordClass'
   * 
   * UPDATE userlogins5.steps
   * SET question_name='InternalQuestions.PopsetRecordClasses_PopsetRecordClassBySnapshotBasket'
   * WHERE question_name = 'InternalQuestions.IsolateRecordClasses_IsolateRecordClassBySnapshotBasket'
   */
  private static final Map<String,String> QUESTION_NAME_REPLACEMENTS =
      new MapBuilder<String, String>()
      .put("InternalQuestions.boolean_question_GeneRecordClasses_GeneRecordClass",
          "InternalQuestions.boolean_question_TranscriptRecordClasses_TranscriptRecordClass")
      .put("InternalQuestions.GeneRecordClasses_GeneRecordClassBySnapshotBasket",
          "InternalQuestions.TranscriptRecordClasses_TranscriptRecordClassBySnapshotBasket")
      .put("InternalQuestions.boolean_question_IsolateRecordClasses_IsolateRecordClass",
          "InternalQuestions.boolean_question_PopsetRecordClasses_PopsetRecordClass")
      .put("InternalQuestions.IsolateRecordClasses_IsolateRecordClassBySnapshotBasket",
          "InternalQuestions.PopsetRecordClasses_PopsetRecordClassBySnapshotBasket")
      .toMap();

  private static boolean fixParamFilterRecordClasses(StepData step) {

    String questionName = step.getQuestionName();
    boolean modifiedByThisMethod = false;

    // apply display_params changes
    String displayParams = step.getParamFilters().toString();
    for (ThreeTuple<String, String, List<String>> change : DISPLAY_PARAM_REPLACEMENTS) {
      if (change.getThird().contains(questionName)) {
        if (displayParams.contains(change.getFirst())) {
          displayParams = displayParams.replaceAll(change.getFirst(), change.getSecond());
          modifiedByThisMethod = true;
        }
      }
    }
    if (modifiedByThisMethod) {
      step.setParamFilters(new JSONObject(displayParams));
    }

    // apply new question names
    for (Entry<String, String> entry : QUESTION_NAME_REPLACEMENTS.entrySet()) {
      if (step.getQuestionName().equals(entry.getKey())) {
        step.setQuestionName(entry.getValue());
        modifiedByThisMethod = true;
        break;
      }
    }

    return modifiedByThisMethod;
  }

  private static boolean removeTxFilterFromNonTxSteps(StepData step, Question question) {
    boolean modified = false;
    if (!TranscriptUtil.isTranscriptQuestion(question)) {
      JSONObject displayParams = step.getParamFilters();
      JSONArray filters = displayParams.getJSONArray(Step.KEY_FILTERS);
      for (int i = 0; i < filters.length(); i++) {
        JSONObject filter = filters.getJSONObject(i);
        String name = filter.getString(FilterOption.KEY_NAME);
        if (MatchedTranscriptFilter.MATCHED_TRANSCRIPT_FILTER_ARRAY_KEY.equals(name)) {
          // illegal; this filter should not be here
          filters.remove(i);
          // decrement to look at the next filter in the list (now at this index)
          i--;
          // set this step as modified
          modified = true;
        }
      }
    }
    return modified;
  }
}
