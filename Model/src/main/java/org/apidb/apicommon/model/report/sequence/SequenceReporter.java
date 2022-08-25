package org.gusdb.wdk.model.report.reporter.sequence;

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
import org.gusdb.wdk.model.report.reporter.bed.BedReporter;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.MalformedURLException;
import org.gusdb.wdk.model.answer.request.AnswerRequest;
import org.gusdb.wdk.model.answer.request.TemporaryResultFactory;

public abstract class SequenceReporter extends StandardReporter {

  private static Logger LOG = Logger.getLogger(SequenceReporter.class);


  private URL bedReporterUrl;

  /*
   * Sequence reporters and bed reporters correspond to each other
   */
  abstract BedReporter correspondingBedReporter();

  /*
   * Per-reporter config: which sequence do the .bed coordinates refer to
   */
  abstract String sequenceType();
  
  /*
   * Sequence retrieval options specified by the user
   * Currently one: basesPerLine
   */
  private JSONObject _configuration;

  @Override
  public SequenceReporter configure(JSONObject configuration) throws ReporterConfigException {
    BedReporter bedReporter = correspondingBedReporter();
    /*
     * TODO not quite the right type:
     *
    bedReporter.setProperties(_properties);
     */
    bedReporter.setAnswerValue(_baseAnswer.clone());
    bedReporter.configure(configuration);
    /*
     * TODO:
     * how to create answerRequest from this reporter?
     *
     */
    AnswerRequest answerRequest = null;
    String id = TemporaryResultFactory.insertTemporaryResult(_baseAnswer.getUser().getUserId(), answerRequest);

    /*
     * TODO:
     * we need a full URL
     * Ryan says getting the base URL of the service is possible through _wdkModel
     * how?
     *
     */
    String scheme = null;
    String ssp = null;
    String fragment = "/temporary-results/" + id;

    try {
      bedReporterUrl = new URI(scheme, ssp, fragment).toURL();
    } catch(URISyntaxException | MalformedURLException e){
      throw new RuntimeException(e);
    }


    super.configure(configuration);
    _configuration = configuration;
    return this;
  }

  @Override
  public String getHttpContentType() {
    if ("plain".equals(_configuration.getString("attachmentType"))) {
      return "text/plain";
    } else {
      return "text/fasta";
    }
  }

  @Override
  public String getDownloadFileName() {
    return getQuestion().getName() + ".fasta";
  }

  @Override
  public void write(OutputStream out) throws WdkModelException {
    /*
     * TODO:
     * create a new service abstracting the sequence retrieval service
     * - set the URL in the startup config
     * - make a class representing the service, init at start up, getter in in WdkModel
     * - add observability on https://plasmodb.org/dashboard/?p=Configuration
     */
    Object sequenceRetrievalService = null;


    /*
     * TODO:
     * write an API method for sequenceRetrievalService
     * pass stuff that changes from request to request: sequence type, callback URL, and user-supplied options
     * fix everything else. e.g. uploadMethod = "url"
     * try sync endpoints first
     */
//    InputStream in = sequenceRetrievalService.submitRequestWithCallbackUrl(sequenceType(), bedReporterUrl, _configuration);

    /*
     * TODO:
     * this might be more complicated - follow the streaming logic in FastaReporter 
     */
//    in.transferTo(out);
  }
}
