package org.apidb.apicommon.model.report;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.FormatUtil.Style;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.report.Reporter;
import org.json.JSONObject;

public abstract class FastaReporter extends Reporter {

  private static final Logger LOG = Logger.getLogger(FastaReporter.class);

  protected abstract String getSrtToolUri();

  FastaReporter(AnswerValue answerValue, int startIndex, int endIndex) {
    super(answerValue, startIndex, endIndex);
  }

  private JSONObject _configuration;

  @Override
  public String getConfigInfo() {
    return "undocumented";
  }

  @Override
  public void configure(JSONObject configuration) {
    _configuration = configuration;
  }

  @Override
  protected void initialize() throws WdkModelException {
    // nothing to do here; will open connection in write()
  }

  @Override
  public void write(OutputStream out) throws WdkModelException, WdkUserException {
    try {
     Map<String,String> inputFields = buildFormData(_configuration, wdkModel.getProjectId(), baseAnswer);
     LOG.debug(FormatUtil.prettyPrint(inputFields, Style.MULTI_LINE));
     transferFormResult(inputFields, out);
    }
    catch (IOException e) {
      throw new WdkModelException("Cannot output FASTA reporter data", e);
    }
  }

  private void transferFormResult(Map<String, String> inputFields, OutputStream out)
      throws WdkModelException, IOException {
    Client client = ClientBuilder.newClient();

    // prepare the form
    Form form = new Form();
    for (Entry<String,String> input : inputFields.entrySet()) {
      form.param(input.getKey(), input.getValue());
    }

    Response response = null;
    try {
      response = client.target("http://localhost" + getSrtToolUri()).request(
          MediaType.APPLICATION_OCTET_STREAM_TYPE).post(
              Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
      int status = response.getStatus();
      if (status >= 400)
        throw new WdkModelException("Request failed with status code: " + status);

      InputStream in = response.readEntity(InputStream.class);
      IoUtil.transferStream(out, in);
    }
    finally {
      if (response != null) response.close();
    }
  }

  private Map<String, String> buildFormData(JSONObject configuration, String projectId, AnswerValue answer) throws WdkModelException, WdkUserException {
    Map<String,String> formInputs = new HashMap<>();
    formInputs.put("project_id", projectId);
    for (String key : JSONObject.getNames(configuration)) {
      formInputs.put(key, configuration.get(key).toString());
    }
    formInputs.put("ids", FormatUtil.arrayToString(getIds(answer).toArray(), " "));
    return formInputs;
  }

  private List<String> getIds(AnswerValue answer) throws WdkModelException, WdkUserException {
    List<String> ids = new ArrayList<>();
    List<String[]> primaryKeys = answer.getAllIds();
    for (String[] keys : primaryKeys) {
      
      ids.add(translatePrimaryKeySet(keys));
    }
    return ids;
  }

  protected String translatePrimaryKeySet(String[] primaryKeys) {
    return primaryKeys[0];
  }

  @Override
  protected void complete() {
    // nothing to do here; will close connection in write()
  }

}
