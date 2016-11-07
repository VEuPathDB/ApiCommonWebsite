package org.apidb.apicommon.controller;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apidb.apicommon.model.TranscriptUtil;
import org.gusdb.fgputil.cache.UnfetchableItemException;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.db.runner.SQLRunner.ResultSetHandler;
import org.gusdb.wdk.cache.CacheMgr;
import org.gusdb.wdk.cache.FilterSizeCache.AllSizesFetcher;
import org.gusdb.wdk.cache.FilterSizeCache.FilterSizeGroup;
import org.gusdb.wdk.controller.action.ShowResultSizeAction;
import org.gusdb.wdk.controller.actionutil.ActionUtility;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.AnswerFilterInstance;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.query.Query;
import org.gusdb.wdk.model.query.QueryInstance;
import org.gusdb.wdk.model.user.Step;

public class CustomShowResultSizeAction extends ShowResultSizeAction {

  private static final String CUSTOM_FILTER_SIZE_QUERY_SET = "TranscriptAttributes";
  private static final String CUSTOM_FILTER_SIZE_QUERY_NAME = "transcriptFilterSizes";
  private static final String FILTER_NAME_COLUMN = "filter_name";
  private static final String FILTER_SIZE_COLUMN = "size";
  private static final String ANSWER_FILTER_PARAM_NAME = "species";

  public static class CustomAllSizesFetcher extends AllSizesFetcher {
    
    public CustomAllSizesFetcher(WdkModel wdkModel) {
      super(wdkModel);
    }

    @Override
    public FilterSizeGroup updateItem(Integer stepId, FilterSizeGroup previousVersion)
        throws UnfetchableItemException {
      try {
        Step step = _wdkModel.getStepFactory().getStepById(stepId);
        AnswerValue answerValue = step.getAnswerValue(false);
        if (TranscriptUtil.isTranscriptQuestion(answerValue.getQuestion())) {
          return super.updateItem(stepId, previousVersion);
        }
        previousVersion.sizeMap = getAllFilterDisplaySizes(answerValue, _wdkModel);
        previousVersion.allFiltersLoaded = true;
        return previousVersion;
      }
      catch (WdkUserException | WdkModelException e) {
        throw new UnfetchableItemException(e);
      }
    }
  }

  private static Map<String, Integer> getAllFilterDisplaySizes(AnswerValue answerValue, WdkModel wdkModel)
      throws WdkModelException, WdkUserException {
    Map<String, Integer> queryResults = getSizesFromCustomQuery(answerValue, wdkModel);
    Map<String, Integer> finalResults = new HashMap<>();
    List<String> unfoundFilters = new ArrayList<>();
    // build list of actual results from query results and get list of filters not provided by query
    for (AnswerFilterInstance filterInstance : answerValue.getQuestion().getRecordClass().getFilterInstances()) {
      String filterName = filterInstance.getName();
      if (queryResults.containsKey(filterName)) {
        finalResults.put(filterName, queryResults.get(filterName));
      }
      // hack since query results may contain a count value keyed on filter param value instead of name
      else if (queryResults.containsKey(filterInstance.getParamValueMap().get(ANSWER_FILTER_PARAM_NAME))) {
        finalResults.put(filterName, queryResults.get(filterInstance.getParamValueMap().get(ANSWER_FILTER_PARAM_NAME)));
      }
      else {
        // custom size query did not return result for this filter; add to list of remaining filters
        unfoundFilters.add(filterName);
      }
    }
    // get filter sizes not found by custom query in the traditional way (each costs us a trip to the DB)
    finalResults.putAll(answerValue.getFilterDisplaySizes(unfoundFilters));
    return finalResults;
  }

  private static Map<String, Integer> getSizesFromCustomQuery(AnswerValue answerValue, WdkModel wdkModel)
      throws WdkModelException, WdkUserException {
    Query query = wdkModel.getQuerySet(CUSTOM_FILTER_SIZE_QUERY_SET).getQuery(CUSTOM_FILTER_SIZE_QUERY_NAME);
    QueryInstance<?> queryInstance = query.makeInstance(answerValue.getUser(),
        new LinkedHashMap<String, String>(), true, 0, new LinkedHashMap<String, String>());
    String sql = queryInstance.getSql().replace(Utilities.MACRO_ID_SQL, answerValue.getIdSql());
    final Map<String, Integer> querySizes = new HashMap<>();
    new SQLRunner(wdkModel.getAppDb().getDataSource(), sql).executeQuery(new ResultSetHandler() {
      @Override public void handleResult(ResultSet rs) throws SQLException {
        while (rs.next()) {
          querySizes.put(rs.getString(FILTER_NAME_COLUMN), rs.getInt(FILTER_SIZE_COLUMN));
        }
      }
    });
    return querySizes;
  }

  // no filter is specified, will return all (legacy) filter sizes for the given step
  @Override
  protected String getFilterResultSizes(int stepId)
      throws WdkModelException, WdkUserException {
    WdkModel wdkModel = ActionUtility.getWdkModel(getServlet()).getModel();
    Map<String, Integer> sizes = CacheMgr.get().getFilterSizeCache()
        .getFilterSizes(stepId, new CustomAllSizesFetcher(wdkModel));
    return getFilterSizesJson(sizes);
  }
}
