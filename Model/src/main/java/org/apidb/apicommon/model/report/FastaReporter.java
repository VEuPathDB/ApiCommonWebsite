package org.apidb.apicommon.model.report;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.iterator.IteratorUtil;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.gusdb.wdk.model.record.PrimaryKeyIterator;
import org.gusdb.wdk.model.report.AbstractReporter;
import org.gusdb.wdk.model.report.config.StandardConfig;
import org.json.JSONObject;

public abstract class FastaReporter extends AbstractReporter {

  private static final Logger LOG = Logger.getLogger(FastaReporter.class);

  protected abstract String getSrtToolUri();

  private JSONObject _configuration;

  @Override
  public FastaReporter configure(JSONObject configuration) {
    _configuration = configuration;
    return this;
  }

  @Override
  public String getDownloadFileName() {
    if (_configuration.has(StandardConfig.ATTACHMENT_TYPE_JSON)) {
      return (_configuration.getString(StandardConfig.ATTACHMENT_TYPE_JSON).equals("text") ?
          getQuestion().getName() + ".fasta" : null);
    }
    // if unspecified, return parent's default
    return super.getDownloadFileName();
  }

  @Override
  public void write(OutputStream out) throws WdkModelException {
    Response response = null;
    try {
      // translate reporter request into a SRT tool request

      // build SRT tool URL
      String baseUrl = _wdkModel.getProperties().get("LOCALHOST");
      String srtToolUri = getSrtToolUri();
      String srtUrl = baseUrl + (srtToolUri.startsWith("/") ? "" : "/") + srtToolUri;
      LOG.info("Submitting form to " + srtUrl);

      // make request
      response = ClientBuilder.newClient()
          .target(srtUrl)
          .request(MediaType.APPLICATION_OCTET_STREAM_TYPE) // response type
          .post(Entity.entity(
              new SrtRequestBodyStream(),
              MediaType.APPLICATION_FORM_URLENCODED_TYPE // request type
          ));

      // read response status and stream response body if successful, else throw 500
      int status = response.getStatus();
      if (status >= 400)
        throw new WdkModelException("Request failed with status code: " + status);

      InputStream in = response.readEntity(InputStream.class);
      IoUtil.transferStream(out, in);
    }
    catch (IOException e) {
      throw new WdkModelException("Cannot output FASTA reporter data", e);
    }
    finally {
      if (response != null) response.close();
    }
  }

  private class SrtRequestBodyStream implements StreamingOutput {

    @Override
    public void write(OutputStream output) throws IOException, WebApplicationException {

      try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output))) {

        // write project ID
        writer.write("project_id=" + _baseAnswer.getWdkModel().getProjectId());

        // write user inputs
        for (String key : JsonUtil.getKeys(_configuration)) {
          writer.write('&');
          writer.write(URLEncoder.encode(key, UTF_8));
          writer.write('=');
          writer.write(URLEncoder.encode(_configuration.get(key).toString(), UTF_8));
        }

        // write IDs
        writer.write("&ids=");
        try (PrimaryKeyIterator ids = _baseAnswer.getAllIds()) {
          // SRT expects IDs in the following URL-encoded format:
          //   Each '\n'-delimited line contains one record, whose
          //   primary keys are joined and delimited by a comma
          for (String[] pk : IteratorUtil.toIterable(ids)) {
            // write each PK
            String row = String.join(", ", pk) + '\n';
            writer.write(URLEncoder.encode(row, UTF_8));
          }
        }
      }
      catch (Exception e) {
        throw new WdkRuntimeException("Unable to stream answer IDs as SRT tool request.", e);
      }
    }
  }

}
