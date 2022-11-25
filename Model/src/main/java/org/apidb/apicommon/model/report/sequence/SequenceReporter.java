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
import org.apidb.apicommon.model.report.bed.BedGeneReporter;
import org.gusdb.wdk.model.report.ReporterConfigException;
import org.json.JSONObject;

public class SequenceReporter extends AbstractReporter {

  private static Logger LOG = Logger.getLogger(SequenceReporter.class);

  private static final String FASTA_MEDIA_TYPE = "text/x-fasta";
  private static final String PLAIN_MEDIA_TYPE = "text/plain";
  private static final String BED_REPORTER_NAME = "bed";

  private enum SequenceType {
    genomic,
    protein,
    est,
    popset;
  }

  private boolean _showInBrowser = false;
  // data required to make sequence retrieval service request
  private String _seqRetSvcRequestUrl;
  private String _bedFileUrl;

  @Override
  public SequenceReporter configure(JSONObject config) throws ReporterConfigException {
    _showInBrowser = "plain".equals(config.getString("attachmentType"));


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

    SequenceType sequenceType;
    try {
      sequenceType = getSequenceTypeByRecordClassFullName(config, getQuestion().getRecordClass().getFullName());
    } catch (WdkModelException e){
      throw new ReporterConfigException("Could not configure reporter", e);
    }

    // FIXME: starting here with synchronous API for proof of concept; convert to async
    _seqRetSvcRequestUrl = localhost + modelProps.get("SEQUENCE_RETRIEVAL_SERVICE_URL") +
        "/sequences/" + sequenceType.name() + "/bed?basesPerLine=" + basesPerLine; 

    LOG.info("Configured sequence reporter to return FASTA.\n  bedFileUrl = " + _bedFileUrl + "\n  seqRetSvcUrl = " + _seqRetSvcRequestUrl);
    return this;
  }

  private static SequenceType getSequenceTypeByRecordClassFullName(JSONObject config, String recordClassFullName)  throws WdkModelException {
    switch(recordClassFullName) {
      case TranscriptUtil.GENE_RECORDCLASS:
      case TranscriptUtil.TRANSCRIPT_RECORDCLASS:
        if(BedGeneReporter.useCoordinatesOnProteinReference(config)){
          return SequenceType.protein;
        } else {
          return SequenceType.genomic;
        }
      case "EstRecordClasses.EstRecordClass":
        return SequenceType.est;
      case "PopsetRecordClasses.PopsetRecordClass":
        return SequenceType.popset;
      case "DynSpanRecordClasses.DynSpanRecordClass":
      case "SequenceRecordClasses.SequenceRecordClass":
        return SequenceType.genomic;
      default:
        throw new WdkModelException(String.format("Unsupported record type: %s", recordClassFullName));
    }
  }

  @Override
  public String getHttpContentType() {
    if(_showInBrowser){
      return PLAIN_MEDIA_TYPE;
    } else {
      return FASTA_MEDIA_TYPE;
    }
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
}
