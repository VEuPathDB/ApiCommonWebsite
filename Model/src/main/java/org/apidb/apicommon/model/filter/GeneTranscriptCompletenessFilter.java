package org.apidb.apicommon.model.filter;

import static org.gusdb.fgputil.FormatUtil.NL;

import java.util.Optional;

import javax.sql.DataSource;

import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.functional.FunctionalInterfaces.BiFunctionWithException;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationBundle.ValidationBundleBuilder;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.answer.spec.SimpleAnswerSpec;
import org.gusdb.wdk.model.filter.StepFilter;
import org.gusdb.wdk.model.question.Question;
import org.json.JSONException;
import org.json.JSONObject;

public class GeneTranscriptCompletenessFilter extends StepFilter {

  public static final String GENE_TRANSCRIPT_COMPLETENESS_FILTER_KEY = "gene_transcript_completeness";

  private static final String MODE_PROP = "mode";

  private enum FilterMode {

    NO_FILTER(
        "Returns the original result",
        (answer, sql) -> sql),

    KEEP_GENES_WITH_ALL_TRANSCRIPTS(
        "Filters out transcripts belonging to genes for which not all transcripts are in the result.",
        GeneTranscriptCompletenessFilter::getGenesWithAllTranscriptsSql),

    KEEP_GENES_WITH_MISSING_TRANSCRIPTS(
        "Filters out transcript belonging to genes for which all transcripts are in the result.",
        GeneTranscriptCompletenessFilter::getGenesWithMissingTranscriptsSql),

    FIND_MISSING_TRANSCRIPTS(
        "Finds transcripts missing from genes in the result",
        GeneTranscriptCompletenessFilter::getMissingTranscriptsSql);

    private final String _description;
    private final BiFunctionWithException<AnswerValue, String, String> _filterSqlFunction;

    private FilterMode(String description, BiFunctionWithException<AnswerValue, String, String> filterSqlFunction) {
      _description = description;
      _filterSqlFunction = filterSqlFunction;
    }

    public String getDescription() {
      return _description;
    }

    public BiFunctionWithException<AnswerValue, String, String> getFilterSqlFunction() {
      return _filterSqlFunction;
    }
  }

  @Override
  public String getKey() {
    return GENE_TRANSCRIPT_COMPLETENESS_FILTER_KEY;
  }

  private static Optional<FilterMode> getFilterMode(JSONObject config) {
    try {
      String modeStr = config.optString(MODE_PROP, FilterMode.KEEP_GENES_WITH_ALL_TRANSCRIPTS.name());
      return Optional.of(FilterMode.valueOf(modeStr.toUpperCase()));
    }
    catch (JSONException | IllegalArgumentException | NullPointerException e) {
      return Optional.empty();
    }
  }

  @Override
  public JSONObject getSummaryJson(AnswerValue answer, String idSql) throws WdkModelException {
    DataSource ds = answer.getWdkModel().getAppDb().getDataSource();
    return WdkModelException.wrap(() -> {
      JSONObject result = new JSONObject();
      for (FilterMode mode : FilterMode.values()) {
        result.put(mode.name().toLowerCase(), getFilteredResultCounts(ds, mode.getFilterSqlFunction().apply(answer, idSql)));
      }
      return result;
    });
  }

  @Override
  public String getDisplay() {
    return "Filter based on whether all transcripts of a genes are present in the result";
  }

  @Override
  public String getDisplayValue(AnswerValue answer, JSONObject jsValue) throws WdkModelException {
    return getFilterMode(jsValue).map(FilterMode::getDescription).orElseThrow();
  }

  @Override
  public String getSql(AnswerValue answer, String idSql, JSONObject jsValue) throws WdkModelException {
    FilterMode mode = getFilterMode(jsValue).orElseThrow();
    return WdkModelException.wrap(() -> mode.getFilterSqlFunction().apply(answer, idSql));
  }

  @Override
  public boolean defaultValueEquals(SimpleAnswerSpec spec, JSONObject jsValue) throws WdkModelException {
    return jsValue == null;
  }

  @Override
  public JSONObject getDefaultValue(SimpleAnswerSpec spec) {
    // default if filter is present is keep-with-all-transcripts, but default is for no filter at all
    return null;
  }

  @Override
  public ValidationBundle validate(Question question, JSONObject value, ValidationLevel validationLevel) {
    ValidationBundleBuilder validation = ValidationBundle.builder(validationLevel);
    Optional<FilterMode> mode = getFilterMode(value);
    if (mode.isEmpty() ||
        mode.get() == FilterMode.NO_FILTER || // does not make sense to make a call to no filter; just omit the filter completely
        mode.get() == FilterMode.FIND_MISSING_TRANSCRIPTS) { // disallow external calls to missing transcripts mode since it is not a "filter"
      validation.addError("Config for filter " + getKey() + " must contain property " + MODE_PROP +
          " with value " + FilterMode.KEEP_GENES_WITH_ALL_TRANSCRIPTS.name() + " or " + FilterMode.KEEP_GENES_WITH_MISSING_TRANSCRIPTS.name());
    }
    return validation.build();
  }

  private static String getGenesWithAllTranscriptsSql(AnswerValue answer, String idSql) {
    return idSql;
  }

  private static String getGenesWithMissingTranscriptsSql(AnswerValue answer, String idSql) {
    return idSql;
  }

  private static String getMissingTranscriptsSql(AnswerValue answer, String idSql) {
    return idSql;
  }

  private JSONObject getFilteredResultCounts(DataSource ds, String filterSql) {
    String sql =
        "WITH filteredsql AS ( " + filterSql + " )" + NL +
        " select 'transcripts' as type, count(1) as num from filteredsql" + NL +
        " union" + NL +
        " select 'genes' as type, count(1) as num from (select distinct gene_source_id from filteredsql)";
    return new SQLRunner(ds, sql, "transcript-completeness-counts").executeQuery(rs -> {
      JSONObject counts = new JSONObject();
      while (rs.next()) {
        counts.put(rs.getString("type"), rs.getLong("num"));
      }
      return counts;
    });
  }
}
