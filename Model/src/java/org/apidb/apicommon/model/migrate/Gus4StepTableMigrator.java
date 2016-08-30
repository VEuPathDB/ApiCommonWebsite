package org.apidb.apicommon.model.migrate;

import static org.apidb.apicommon.model.filter.FilterValueArrayUtil.getFilterValueArray;

import java.util.Map;
import java.util.Set;

import org.apidb.apicommon.model.filter.GeneBooleanFilter;
import org.apidb.apicommon.model.filter.MatchedTranscriptFilter;
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

  private static final String GENE_RECORDCLASS = "GeneRecordClasses.GeneRecordClass";
  private static final String TRANSCRIPT_RECORDCLASS = "TranscriptRecordClasses.TranscriptRecordClass";
  private static final String USE_BOOLEAN_FILTER_PARAM = "use_boolean_filter";

  @Override
  public TableRowUpdater<StepData> getTableRowUpdater(WdkModel wdkModel) {
    return new TableRowUpdater<StepData>(new StepDataFactory(false), this, wdkModel);
  }

  @Override
  public RowResult<StepData> processRecord(StepData step, WdkModel wdkModel) throws WdkModelException {
    RowResult<StepData> result = new RowResult<>(false, step);
    Question question = wdkModel.getQuestion(step.getQuestionName());
    RecordClass recordClass = question.getRecordClass();
    boolean isBoolean = question.getQuery().isBoolean();
    boolean isTransform = question.getQuery().isTransform();
    boolean isLeaf = !isBoolean && !isTransform;
    
    // 1. Add "filters" property if not present and convert any found objects to filter array
    updateFiltersProperty(result);

    // 2. Add matched transcript filter to all leaf transcript steps
    addMatchedTranscriptFilter(result, isLeaf, recordClass);

    // 3. Add gene boolean filter to all boolean transcript steps
    addGeneBooleanFilter(result, isBoolean, recordClass);

    // 4. Remove use_boolean_filter param when found
    removeUseBooleanFilterParam(result, isBoolean);

    // 5. For parameters that are filterParams: convert value "Unknown" to null
    removeUnknownFilterParamValues(result, question);

    return result;
  }

  private static void removeUnknownFilterParamValues(RowResult<StepData> result, Question question) throws WdkModelException {
    JSONObject params = result.getTableRow().getParamFilters().getJSONObject(Step.KEY_PARAMS);
    Map<String, Param> qParams = question.getParamMap();
    Set<String> paramNames  = params.keySet();
    for (String paramName : paramNames) {
      if (!qParams.containsKey(paramName)) {
        throw new WdkModelException("Step " + result.getTableRow().getStepId() + " does not contain required param " + paramName);
      }
      Param param = qParams.get(paramName);
      if (!(param instanceof FilterParam)) {
        continue;
      }
      String value = params.getString(paramName);
      if (!"Unknown".equals(value)) {
        continue;
      }
      params.put(paramName, "null");
      result.setModified();
    }
  }

  private static void removeUseBooleanFilterParam(RowResult<StepData> result, boolean isBoolean) {
    if (!isBoolean) return;
    JSONObject params = result.getTableRow().getParamFilters().getJSONObject(Step.KEY_PARAMS);
    if (!params.has(USE_BOOLEAN_FILTER_PARAM)) return;
    params.remove(USE_BOOLEAN_FILTER_PARAM);
    result.setModified();
  }

  private static void addGeneBooleanFilter(RowResult<StepData> result, boolean isBoolean, RecordClass recordClass) throws WdkModelException, JSONException {
    StepData step = result.getTableRow();
    if (!isBoolean) return;
    if (!recordClass.getFullName().equals(GENE_RECORDCLASS)) return;
    // figure out default value based on boolean param
    JSONObject params = step.getParamFilters().getJSONObject(Step.KEY_PARAMS);
    boolean isWdkSetOperation = params.has(BooleanQuery.OPERATOR_PARAM);
    if (!isWdkSetOperation) return;
    JSONObject defaultValue = GeneBooleanFilter.getDefaultValue(params.getString(BooleanQuery.OPERATOR_PARAM));
    if (defaultValue == null) return;
    boolean modified = addFilterValueArray(step, GeneBooleanFilter.GENE_BOOLEAN_FILTER_ARRAY_KEY, defaultValue);
    if (modified) result.setModified();
  }

  private static void addMatchedTranscriptFilter(RowResult<StepData> result, boolean isLeaf, RecordClass recordClass) {
    // requirements:
    //   transcript question
    //   leaf step
    //   non-basket
    StepData step = result.getTableRow();
    if (!isLeaf) return;
    if (!recordClass.getFullName().equals(TRANSCRIPT_RECORDCLASS)) return;
    if (step.getQuestionName().toLowerCase().contains("basket")) return;
    // add filter with default value if not already present
    JSONObject defaultValue = getFilterValueArray("Y");
    boolean modified = addFilterValueArray(step, MatchedTranscriptFilter.MATCHED_TRANSCRIPT_FILTER_ARRAY_KEY, defaultValue);
    if (modified) result.setModified();
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

  private static void updateFiltersProperty(RowResult<StepData> result) {
    try {
      JsonType json = new JsonType(result.getTableRow().getParamFilters().get(Step.KEY_FILTERS).toString());
      if (json.getNativeType().equals(NativeType.OBJECT)) {
        // need to convert to array
        result.getTableRow().getParamFilters().put(Step.KEY_FILTERS, new JSONArray());
        result.setModified();
      }
    }
    catch (JSONException e) {
      // means filter value not present
      result.getTableRow().getParamFilters().put(Step.KEY_FILTERS, new JSONArray());
      result.setModified();
    }
  }
}
