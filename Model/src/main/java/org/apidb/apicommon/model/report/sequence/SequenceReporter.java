package org.apidb.apicommon.model.report.sequence;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apidb.apicommon.model.TranscriptUtil;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.client.ClientUtil;
import org.gusdb.fgputil.client.ResponseFuture;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.answer.request.AnswerFormatting;
import org.gusdb.wdk.model.answer.request.AnswerRequest;
import org.gusdb.wdk.model.answer.request.TemporaryResultFactory;
import org.gusdb.wdk.model.report.AbstractReporter;
import org.gusdb.wdk.model.report.ReporterConfigException;
import org.json.JSONObject;

public class SequenceReporter extends AbstractReporter {

  private static Logger LOG = Logger.getLogger(SequenceReporter.class);

  private static final String FASTA_MEDIA_TYPE = "text/x-fasta";
  private static final String BED_REPORTER_NAME = "bed";

  private enum SequenceType {
    genomic,
    protein;
  }

  // data required to make sequence retrieval service request
  private String _seqRetSvcRequestUrl;
  private String _bedFileUrl;

  @Override
  public SequenceReporter configure(JSONObject config) throws ReporterConfigException {

    // extract any sequence retrieval service config from this config
    int basesPerLine = config.optInt("basesPerLine", 60);

    // build an answer request to save off and create a temporary result URL for
    AnswerRequest bedReporterRequest = new AnswerRequest(_baseAnswer.getRunnableAnswerSpec(), new AnswerFormatting(BED_REPORTER_NAME, config), false);
    String bedResultId = TemporaryResultFactory.insertTemporaryResult(_baseAnswer.getUser().getUserId(), bedReporterRequest);

    // Determine needed URLs from model props
    //   Example prop values:
    //     LOCALHOST=https://rdoherty.plasmodb.org
    //     SERVICE_BASE_URL=/plasmo.rdoherty/service
    //     SEQUENCE_RETRIEVAL_SERVICE_URL=/sequence-retrieval
    Map<String,String> modelProps = _baseAnswer.getWdkModel().getProperties();
    String localhost = modelProps.get("LOCALHOST");
    _bedFileUrl = localhost + modelProps.get("SERVICE_BASE_URL") + "/temporary-results/" + bedResultId;

    // FIXME: starting here with synchronous API for proof of concept; convert to async
    SequenceType sequenceType = getSequenceTypeByRecordClassFullName(config, getQuestion().getRecordClass().getFullName());
    _seqRetSvcRequestUrl = localhost + modelProps.get("SEQUENCE_RETRIEVAL_SERVICE_URL") +
        "/sequences/" + sequenceType.name() + "/bed?basesPerLine=" + basesPerLine; 

    LOG.info("Configured sequence reporter to return FASTA.\n  bedFileUrl = " + _bedFileUrl + "\n  seqRetSvcUrl = " + _seqRetSvcRequestUrl);
    return this;
  }

  private static SequenceType getSequenceTypeByRecordClassFullName(JSONObject config, String recordClassFullName) {
    // FIXME: put the correct mapping here; return protein where appropriate
    switch(recordClassFullName) {
      case TranscriptUtil.GENE_RECORDCLASS:
      case TranscriptUtil.TRANSCRIPT_RECORDCLASS:
      case "DynSpanRecordClasses.DynSpanRecordClass":
      case "EstRecordClasses.EstRecordClass":
      case "SequenceRecordClasses.SequenceRecordClass":
      case "PopsetRecordClasses.PopsetRecordClass":
      default:
        return SequenceType.genomic;
    }
  }

  @Override
  public String getHttpContentType() {
    return FASTA_MEDIA_TYPE;
  }

  @Override
  public String getDownloadFileName() {
    return getQuestion().getName() + ".fasta";
  }

  @Override
  public void write(OutputStream out) throws WdkModelException {
    /*
     * TODO: Future async responses will contain a job ID which can be used to query progress until
     * complete.  Then a follow-up call must be made to get the result.  We might want to consider
     * doing the polling/waiting above in configure(), because if it fails or times out, we can
     * still throw a 4xx/5xx.  Once in write(), we are stuck with 2xx.  Drawback is that the response
     * object will need to be closed, and we can't do a try-with-resources across methods.  TBR.
     */
    @SuppressWarnings("resource")
    MultiPart seqRetSvcRequestBody = new FormDataMultiPart()
        .field("uploadMethod", "url")
        .field("url", _bedFileUrl);

    // make call to sequence retrieval service
    ResponseFuture response = ClientUtil.makeAsyncMultiPartPostRequest(
        _seqRetSvcRequestUrl, seqRetSvcRequestBody, FASTA_MEDIA_TYPE, Collections.emptyMap());

    // wait for response, then read and stream out to client
    try (InputStream successfulResponseBody = response.getInputStream()) {
      IoUtil.transferStream(out, successfulResponseBody);
    }
    catch (Exception e) {
      throw new WdkModelException("Unable to stream response", e);
    }
    finally {
      IoUtil.closeQuietly(seqRetSvcRequestBody);
    }
  }

  // Wojtek: leaving your notes here for your reference; I don't think we need any of this, however
  /*
   * TODO:
   * create a new service abstracting the sequence retrieval service
   * - set the URL in the startup config
   * - make a class representing the service, init at start up, getter in in WdkModel
   * - add observability on https://plasmodb.org/dashboard/?p=Configuration
   */
  /*
   * TODO:
   * write an API method for sequenceRetrievalService
   * pass stuff that changes from request to request: sequence type, callback URL, and user-supplied options
   * fix everything else. e.g. uploadMethod = "url"
   * try sync endpoints first
   */
  //    InputStream in = sequenceRetrievalService.submitRequestWithCallbackUrl(sequenceType(), bedReporterUrl, _configuration);

}
