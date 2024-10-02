package org.apidb.apicommon.model.report.sequence;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.log4j.Logger;
import org.apidb.apicommon.model.TranscriptUtil;
import org.apidb.apicommon.model.report.bed.BedGeneReporter;
import org.apidb.apicommon.model.report.bed.BedReporter;
import org.eupathdb.common.service.PostValidationUserException;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.client.ClientUtil;
import org.gusdb.fgputil.client.RequestFailure;
import org.gusdb.fgputil.client.ResponseFuture;
import org.gusdb.fgputil.functional.Either;
import org.gusdb.fgputil.functional.Functions;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.gusdb.wdk.model.answer.request.AnswerFormatting;
import org.gusdb.wdk.model.answer.request.AnswerRequest;
import org.gusdb.wdk.model.answer.request.TemporaryResultFactory;
import org.gusdb.wdk.model.report.AbstractReporter;
import org.gusdb.wdk.model.report.ReporterConfigException;
import org.json.JSONObject;

public class SequenceReporter extends AbstractReporter {

  private static Logger LOG = Logger.getLogger(SequenceReporter.class);

  private static final String FASTA_MEDIA_TYPE = "text/plain";
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
    _showInBrowser = "plain".equals(config.optString("attachmentType", "plain"));

    // extract any sequence retrieval service config from this config
    int basesPerLine;
    if("fixed_width".equals(config.getString("sequenceFormat"))){
      basesPerLine = config.getInt("basesPerLine");
    } else {
      basesPerLine = 0;
    }

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
        "/sequences/" + sequenceType.name() + "/bed?basesPerLine=" + basesPerLine + "&deflineFormat=QUERYONLY&startOffset=ONE"; 

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
    return FASTA_MEDIA_TYPE;
  }

  @Override
  public String getDownloadFileName() {
    // null filename will indicate inline contentDisposition
    return _showInBrowser ? null : getQuestion().getName() + ".fasta";
  }

  @Override
  public void write(OutputStream out) throws WdkModelException {

    // if bed file does not contain any features, skip call to
    //   seqret service and write standard message instead
    if (isWriteEmptyBedFileResponse(out)) {
      return;
    }

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
    ResponseFuture responseFuture = ClientUtil.makeAsyncMultiPartPostRequest(
        _seqRetSvcRequestUrl, seqRetSvcRequestBody, MediaType.WILDCARD, Collections.emptyMap());

    // wait for response, then read into an Either
    Either<InputStream, RequestFailure> response = Functions.mapException(() -> responseFuture.getEither(),
        e -> new RuntimeException("Unable to receive response from sequence retrieval (service may be down)", e));

    response
    // handle success case
    .ifLeft(successfulResponseBody -> {
      try {
        IoUtil.transferStream(out, successfulResponseBody);
      }
      catch (IOException e) {
        throw new WdkRuntimeException("Unable to stream response", e);
      }
      finally {
        IoUtil.closeQuietly(seqRetSvcRequestBody);
      }
    })
    // handle failure case
    .ifRight(failure -> {
      if (failure.getStatusType().getFamily().equals(Family.CLIENT_ERROR)) {
        throw new PostValidationUserException(failure.getResponseBody());
      }
      else {
        LOG.error("Received 5xx response from sequence retrieval service with body: \n" + failure.getResponseBody());
        String failureResponseBody = failure.getResponseBody();
        if (failureResponseBody.contains("502 Proxy Error")) {
          throw new PostValidationUserException("This request has timed out.  If this problem persists, please contact us.");
        }
        throw new RuntimeException(failureResponseBody);
      }
    });
  }

  private boolean isWriteEmptyBedFileResponse(OutputStream out) {
    LOG.info("Will preview result to check for empty bed file at URL: " + _bedFileUrl);
    try (InputStream in = new URL(_bedFileUrl).openStream()) {
      int firstChar = in.read();
      if (firstChar == -1) {
        throw new RuntimeException("BedReporter returned empty response.");
      }
      LOG.info("First char = " + (char)firstChar);
      if (firstChar == BedReporter.EMPTY_FEATURE_OUTPUT.charAt(0)) {
        // no bed features found in this result, write empty result as sequence reporter result as well
        out.write(BedReporter.EMPTY_FEATURE_OUTPUT.getBytes());
        out.flush();
        return true;
      }
      return false;
    }
    catch (IOException e) {
      throw new RuntimeException("Unable to return empty result", e);
    }
  }
}
