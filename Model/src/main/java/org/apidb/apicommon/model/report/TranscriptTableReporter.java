package org.apidb.apicommon.model.report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apidb.apicommon.model.TranscriptUtil;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.stream.RecordStream;
import org.gusdb.wdk.model.answer.stream.SingleTableRecordStream;
import org.gusdb.wdk.model.query.param.FlatVocabParam;
import org.gusdb.wdk.model.report.ReporterConfigException;
import org.gusdb.wdk.model.report.reporter.TableTabularReporter;
import org.json.JSONArray;
import org.json.JSONObject;

public class TranscriptTableReporter extends TableTabularReporter {

  private String _originalQuestionName;
  private String _customTableSql;

  @Override
  public TranscriptTableReporter configure(Map<String, String> config) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TranscriptTableReporter configure(JSONObject config) throws ReporterConfigException {
    try {
      _originalQuestionName = _baseAnswer.getQuestion().getName();
      _baseAnswer = TranscriptUtil.transformToGeneAnswer(_baseAnswer);

      // now that base answer is a Gene answer, check and assign selected table field name
      super.configure(config);

      // need special processing for orthologs table
      if (_tableField.getName().equals("OrthologsLite")) {

        // look up ortholog organism option on config and convert to abbrevs
        List<String> orgAbbrevs = findOrgAbbrevs(config);

        // use ortholog table with organism filtering based on reporter config
        _tableField = _baseAnswer.getQuestion().getRecordClass().getTableFieldMap().get("OrthologsLiteForDownload");

        // build custom table SQL
        String rawSql = _baseAnswer.getTableFieldResultSql(_tableField);
        _customTableSql = rawSql.replace("$$orthologPartitionKeys$$", orgAbbrevs.stream().collect(Collectors.joining("','","'","'")));

      }
      return this;
    }
    catch (WdkUserException e) {
      throw new ReporterConfigException(e.getMessage());
    }
    catch (WdkModelException e) {
      throw new WdkRuntimeException("Could not create in-memory step from incoming answer spec", e);
    }
  }

  private List<String> findOrgAbbrevs(JSONObject config) throws ReporterConfigException, WdkModelException {

    // look for org filtering parameter on reporter config
    JSONArray orgsJson = config.optJSONArray("orthologOrganisms");
    if (orgsJson == null)
      throw new ReporterConfigException("OrthologsLite table requires an additional config value 'orthologOrganisms', an array of organism terms");
    List<String> orgTerms = Arrays.asList(JsonUtil.toStringArray(orgsJson));

    // look up term to internal map for organism param; internal value is org_abbrev
    FlatVocabParam orgParam = (FlatVocabParam)_baseAnswer.getWdkModel().getQuestionByName("GenesByTaxon").get().getParamMap().get("organism");
    Map<String,String> termsToInternalMap = orgParam.getVocabInstance(_baseAnswer.getRequestingUser(), Collections.emptyMap()).getVocabMap();

    // convert terms to internal values
    List<String> orgAbbrevs = new ArrayList<>();
    for (String term : orgTerms) {
      String internal = termsToInternalMap.get(term);
      if (internal == null) {
        throw new ReporterConfigException("At least one passed ortholog organsim is invalid ('" + term + "').");
      }
      orgAbbrevs.add(internal);
    }
    return orgAbbrevs;
  }

  @Override
  protected RecordStream getSingleTableRecordStream() throws WdkModelException {
    return _customTableSql == null
      ? super.getSingleTableRecordStream()
      : new SingleTableRecordStream(_baseAnswer, _tableField, _customTableSql);
  }

  @Override
  public String getDownloadFileName() {
    return getDownloadFileName(_originalQuestionName);
  }
}
