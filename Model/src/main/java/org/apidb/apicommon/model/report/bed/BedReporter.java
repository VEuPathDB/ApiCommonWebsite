package org.gusdb.wdk.model.report.reporter.bed;

import java.util.List;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.json.JSONObject;
import org.apache.log4j.Logger;
import org.gusdb.wdk.model.answer.stream.RecordStream;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.report.ReporterConfigException;
import org.gusdb.wdk.model.report.reporter.StandardReporter;

public abstract class BedReporter extends StandardReporter {

  private static Logger LOG = Logger.getLogger(BedReporter.class);

  private JSONObject _configuration;

  @Override
  public BedReporter configure(JSONObject configuration) throws ReporterConfigException {
    super.configure(configuration);
    _configuration = configuration;
    return this;
  }

  @Override
  public String getHttpContentType() {
    if ("plain".equals(_configuration.getString("attachmentType"))) {
      return "text/plain";
    } else {
      return "text/tsv";
    }
  }

  @Override
  public String getDownloadFileName() {
    return getQuestion().getName() + ".tsv";
  }

  //  spec: https://en.wikipedia.org/wiki/BED_(file_format)
  //  return one or more lines
  protected abstract List<List<String>> recordAsBedFields(JSONObject configuration, RecordInstance record);

  @Override
  public void write(OutputStream out) throws WdkModelException {
    RecordStream records = getRecords();
    int recordCount = 0;
    PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
    for (RecordInstance record : records) {
      recordCount++;
      for (List<String> line: recordAsBedFields(_configuration, record)){
        writer.println(String.join("\t", line));
      }
      writer.flush();
    }
    LOG.info("Wrote " + recordCount + " records");
  }
}

