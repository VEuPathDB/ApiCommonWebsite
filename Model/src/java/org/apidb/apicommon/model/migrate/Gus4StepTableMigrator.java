package org.apidb.apicommon.model.migrate;

import static org.apidb.apicommon.model.filter.FilterValueArrayUtil.getFilterValueArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.apidb.apicommon.model.filter.GeneBooleanFilter;
import org.apidb.apicommon.model.filter.MatchedTranscriptFilter;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.JsonType;
import org.gusdb.fgputil.JsonType.NativeType;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.filter.FilterOption;
import org.gusdb.wdk.model.fix.table.TableRowInterfaces.RowResult;
import org.gusdb.wdk.model.fix.table.TableRowInterfaces.TableRowUpdaterPlugin;
import org.gusdb.wdk.model.fix.table.TableRowUpdater;
import org.gusdb.wdk.model.fix.table.tables.StepData;
import org.gusdb.wdk.model.fix.table.tables.StepDataFactory;
import org.gusdb.wdk.model.query.BooleanQuery;
import org.gusdb.wdk.model.query.param.FilterParam;
import org.gusdb.wdk.model.query.param.Param;
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

  private static final String TRANSCRIPT_RECORDCLASS = "TranscriptRecordClasses.TranscriptRecordClass";
  private static final String USE_BOOLEAN_FILTER_PARAM = "use_boolean_filter";

  private static final AtomicInteger INVALID_STEP_COUNT_QUESTION = new AtomicInteger(0);
  private static final AtomicInteger INVALID_STEP_COUNT_PARAMS = new AtomicInteger(0);

  @Override
  public TableRowUpdater<StepData> getTableRowUpdater(WdkModel wdkModel) {
    return new TableRowUpdater<StepData>(new StepDataFactory(false), this, wdkModel);
  }

  @Override
  public RowResult<StepData> processRecord(StepData step, WdkModel wdkModel) throws WdkModelException {
    RowResult<StepData> result = new RowResult<>(false, step);
    Question question;
    try {
      question = wdkModel.getQuestion(step.getQuestionName());
    }
    catch (WdkModelException e) {
      if (LOG_INVALID_STEPS)
        LOG.warn("Question name " + step.getQuestionName() + " does not appear in the WDK model (" +
            INVALID_STEP_COUNT_QUESTION.incrementAndGet() + " total invalid steps by question).");
      return result;
    }
    RecordClass recordClass = question.getRecordClass();
    boolean isBoolean = question.getQuery().isBoolean();
    boolean isLeaf = !question.getQuery().isCombined();
    List<String> mods = new ArrayList<>();

    // 0. If "params" prop not present then place entire paramFilters inside and write back
    if (updateParamsProperty(result)) mods.add("updateParams");

    // 1. Add "filters" property if not present and convert any found objects to filter array
    if (updateFiltersProperty(result, Step.KEY_FILTERS)) mods.add("fixFilters");
    if (updateFiltersProperty(result, Step.KEY_VIEW_FILTERS)) mods.add("addViewFilters");

    // 2. Add matched transcript filter to all leaf transcript steps
    if (addMatchedTranscriptFilter(result, isLeaf, recordClass)) mods.add("matchedTxFilter");

    // 3. Add gene boolean filter to all boolean transcript steps
    if (addGeneBooleanFilter(result, isBoolean, recordClass)) mods.add("geneBoolFilter");

    // 4. Remove use_boolean_filter param when found
    if (removeUseBooleanFilterParam(result, isBoolean)) mods.add("useBoolFilter");

    // 5. For parameters that are filterParams: convert value "Unknown" to null
    if (removeUnknownFilterParamValues(result, question)) mods.add("removeUnknown");

    if (result.isModified()) {
      LOG.info("Step modified by " + FormatUtil.arrayToString(mods.toArray()));
      LOG.info("Incoming paramFilters: " + new JSONObject(step.getOrigParamFiltersString()).toString(2));
      LOG.info("Outgoing paramFilters: " + step.getParamFilters().toString(2));
    }
    return result;
  }

  private static boolean updateParamsProperty(RowResult<StepData> result) {
    JSONObject paramFilters = result.getTableRow().getParamFilters();
    if (paramFilters.has(Step.KEY_PARAMS)) return false;
    JSONObject newParamFilters = new JSONObject();
    newParamFilters.put(Step.KEY_PARAMS, paramFilters);
    result.getTableRow().setParamFilters(newParamFilters);
    result.setModified();
    return true;
  }

  private static boolean removeUnknownFilterParamValues(RowResult<StepData> result, Question question) {
    StepData step = result.getTableRow();
    JSONObject params = step.getParamFilters().getJSONObject(Step.KEY_PARAMS);
    
    Map<String, Param> qParams = question.getParamMap();
    
    Set<String> paramNames  = params.keySet();
    for (String paramName : paramNames) {
      if (!qParams.containsKey(paramName)) {
        if (LOG_INVALID_STEPS)
          LOG.warn("Step " + result.getTableRow().getStepId() +
              " contains param " + paramName + ", no longer required by question " +
              question.getFullName() + "(" + INVALID_STEP_COUNT_PARAMS.incrementAndGet() +
              " invalid steps by param).");
        return false;
      }
      Param param = qParams.get(paramName);
      if (!(param instanceof FilterParam)) {
        continue;
      }
      String value = params.getString(paramName);
      JSONObject filterParamValue = new JSONObject(value);
      JSONArray valueFilters = filterParamValue.getJSONArray("filters");
      for (int i = 0; i < valueFilters.length(); i++) {
        JSONObject obj = valueFilters.getJSONObject(i);
        Object valuesObject = obj.get("values");
        if (valuesObject instanceof JSONArray) {
          JSONArray valuesArray = (JSONArray)valuesObject;
          for (int j = 0; j < valuesArray.length(); j++) {
            Object nestedValue = valuesArray.get(j);
            if (nestedValue instanceof String && "Unknown".equals(nestedValue)) {
              valuesArray.put(j, JSONObject.NULL);
              result.setModified();
            }
          }
        }
      }
    }
    return result.isModified();
  }

  private static boolean removeUseBooleanFilterParam(RowResult<StepData> result, boolean isBoolean) {
    if (!isBoolean) return false;
    JSONObject params = result.getTableRow().getParamFilters().getJSONObject(Step.KEY_PARAMS);
    if (!params.has(USE_BOOLEAN_FILTER_PARAM)) return false;
    params.remove(USE_BOOLEAN_FILTER_PARAM);
    result.setModified();
    return true;
  }

  private static boolean addGeneBooleanFilter(RowResult<StepData> result, boolean isBoolean, RecordClass recordClass) throws WdkModelException, JSONException {
    StepData step = result.getTableRow();
    if (!isBoolean) return false;
    if (!recordClass.getFullName().equals(TRANSCRIPT_RECORDCLASS)) return false;
    // figure out default value based on boolean param
    JSONObject params = step.getParamFilters().getJSONObject(Step.KEY_PARAMS);
    boolean isWdkSetOperation = params.has(BooleanQuery.OPERATOR_PARAM);
    if (!isWdkSetOperation) {
      LOG.warn("Found boolean step (ID " + step.getStepId() + ") that does not have param " + BooleanQuery.OPERATOR_PARAM);
      return false;
    }
    JSONObject defaultValue = GeneBooleanFilter.getDefaultValue(params.getString(BooleanQuery.OPERATOR_PARAM));
    if (defaultValue == null) return false;
    boolean modified = addFilterValueArray(step, GeneBooleanFilter.GENE_BOOLEAN_FILTER_ARRAY_KEY, defaultValue);
    if (modified) result.setModified();
    return result.isModified();
  }

  private static boolean addMatchedTranscriptFilter(RowResult<StepData> result, boolean isLeaf, RecordClass recordClass) {
    // requirements:
    //   transcript question
    //   leaf step
    //   non-basket
    StepData step = result.getTableRow();
    if (!isLeaf) return false;
    if (!recordClass.getFullName().equals(TRANSCRIPT_RECORDCLASS)) return false;
    if (step.getQuestionName().toLowerCase().contains("basket")) return false;
    // add filter with default value if not already present
    JSONObject defaultValue = getFilterValueArray("Y");
    boolean modified = addFilterValueArray(step, MatchedTranscriptFilter.MATCHED_TRANSCRIPT_FILTER_ARRAY_KEY, defaultValue);
    if (modified) result.setModified();
    return result.isModified();
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

  private static boolean updateFiltersProperty(RowResult<StepData> result, String filtersKey) {
    try {
      JsonType json = new JsonType(result.getTableRow().getParamFilters().get(filtersKey).toString());
      if (json.getNativeType().equals(NativeType.OBJECT)) {
        // need to convert to array
        result.getTableRow().getParamFilters().put(filtersKey, new JSONArray());
        result.setModified();
        return true;
      }
      return false;
    }
    catch (JSONException e) {
      // means filter value not present
      result.getTableRow().getParamFilters().put(filtersKey, new JSONArray());
      result.setModified();
      return true;
    }
  }
}
