package org.apidb.apicommon.model.report.summaryview;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.db.SqlUtils;
import org.gusdb.fgputil.json.JsonWriter;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.report.AbstractReporter;
import org.gusdb.wdk.model.report.Reporter;
import org.gusdb.wdk.model.report.ReporterConfigException;
import org.json.JSONObject;
import org.json.JSONWriter;

public class IsolateViewReporter extends AbstractReporter {

  public IsolateViewReporter(AnswerValue answerValue) {
    super(answerValue);
  }

  private static final String PROP_SEQUENCES = "isolates";
  private static final String PROP_MAX_LENGTH = "maxLength";

  private static final Logger logger = Logger.getLogger(IsolateViewReporter.class);

  private String prepareSql(String idSql) {
    StringBuilder sql = new StringBuilder("select ");
    sql.append("       cnt.country, cnt.total, cnt.latitude, cnt.longitude, ot.source_id as gaz ");
    sql.append(" from sres.OntologyTerm ot, ");
    sql.append("     (select count(*) as total, latitude, longitude, curated_geographic_location as country ");
    sql.append("      from ApidbTuning.PopsetAttributes ");
    sql.append("      where source_id in (select source_id from ( " + idSql + " )) ");
    sql.append("      group by latitude, longitude, curated_geographic_location) cnt ");
    sql.append("where ot.name = cnt.country ");
    sql.append("  and ot.source_id like 'GAZ%' ");
    return sql.toString();
  }

  @Override
  protected void write(OutputStream out) throws WdkModelException {
    try (JsonWriter writer = new JsonWriter(new BufferedWriter(new OutputStreamWriter(out)))) {
      writeJson(_baseAnswer, writer);
    }
    catch (IOException e) {
      throw new WdkModelException("Unable to write reporter result to output stream", e);
    }

  }

  @Override
  public Reporter configure(JSONObject config) throws ReporterConfigException, WdkModelException {
    return this;
  }

  @Override
  public Reporter configure(Map<String, String> config) throws ReporterConfigException, WdkModelException {
    return this;
  }

  public void writeJson(AnswerValue answerValue, JSONWriter writer) throws WdkModelException {
    logger.debug("Entering IsolateViewHandler...");

    writer.object();

    ResultSet resultSet = null;
    try {
      String sql = prepareSql(answerValue.getIdSql());
      DataSource dataSource = answerValue.getQuestion().getWdkModel().getAppDb().getDataSource();
      resultSet = SqlUtils.executeQuery(dataSource, sql,
          answerValue.getQuestion().getQuery().getFullName() + "__isolate-view", 2000);

      int maxLength = 0;
      Map<String, Isolate> isolates = new HashMap<String, Isolate>();
      while (resultSet.next()) {
        String isolateId = resultSet.getString("country");
        Isolate isolate = isolates.get(isolateId);
        if (isolate == null) {
          isolate = new Isolate(isolateId);
          isolates.put(isolateId, isolate);

          isolate.setTotal(resultSet.getInt("total"));
          isolate.setLat(resultSet.getDouble("latitude"));
          isolate.setLng(resultSet.getDouble("longitude"));
          isolate.setGaz(resultSet.getString("gaz"));
        }
      }

      // sort sequences by source ids
      String[] isolateIds = isolates.keySet().toArray(new String[0]);
      Arrays.sort(isolateIds);
      Isolate[] array = new Isolate[isolateIds.length];
      for (int i = 0; i < isolateIds.length; i++) {
        array[i] = isolates.get(isolateIds[i]);
      }

      writer.key(PROP_SEQUENCES).array();
      for (Isolate isolate : array) {
        isolate.writeJson(writer);
      }
      writer.endArray();
      writer.key(PROP_MAX_LENGTH).value(maxLength);
      writer.endObject();
      logger.debug("Leaving IsolateViewHandler...");
    }
    catch (SQLException | WdkUserException ex) {
      throw new WdkModelException(ex);
    }
    finally {
      SqlUtils.closeResultSetAndStatement(resultSet, null);
    }
  }

}
