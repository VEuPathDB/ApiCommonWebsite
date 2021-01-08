package org.apidb.apicommon.model;

import java.util.Map;
import java.util.Optional;

import org.gusdb.fgputil.MapBuilder;
import org.gusdb.fgputil.validation.ValidObjectFactory.RunnableObj;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.answer.TransformUtil;
import org.gusdb.wdk.model.answer.factory.AnswerValueFactory;
import org.gusdb.wdk.model.answer.spec.AnswerSpec;
import org.gusdb.wdk.model.query.spec.QueryInstanceSpec;
import org.gusdb.wdk.model.question.Question;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.user.StepContainer.ListStepContainer;
import org.gusdb.wdk.model.user.Strategy;
import org.gusdb.wdk.model.user.User;
import org.gusdb.wdk.model.user.UserCache;

public class TranscriptUtil {

  static final String GENE_RECORDCLASS = "GeneRecordClasses.GeneRecordClass";
  static final String TRANSCRIPT_RECORDCLASS = "TranscriptRecordClasses.TranscriptRecordClass";

  private static final String XFORM_QUESTION_NAME = "GeneRecordQuestions.GenesFromTranscripts";
  private static final String XFORM_STEP_ID_PARAM_NAME = "gene_result";

  public static final boolean isGeneRecordClass(String name) {
    return GENE_RECORDCLASS.equals(name);
  }

  public static final boolean isGeneRecordClass(RecordClass recordClass) {
    return isGeneRecordClass(recordClass.getFullName());
  }

  public static final boolean isTranscriptRecordClass(String name) {
    return TRANSCRIPT_RECORDCLASS.equals(name);
  }

  public static final boolean isTranscriptRecordClass(RecordClass recordClass) {
    return isTranscriptRecordClass(recordClass.getFullName());
  }

  public static final boolean isTranscriptQuestion(Question question) {
    return isTranscriptRecordClass(question.getRecordClass());
  }

  /**
   * Takes a transcript answer value and returns an answer value of a transform
   * that will return the genes of the transcripts returned by the input answer.
   *
   * @param transcriptAnswer answer value that returns transcripts
   * @return answer value that will return genes of the transcripts returned by the input answer
   * @throws WdkModelException if error occurs
   */
  public static AnswerValue transformToGeneAnswer(AnswerValue transcriptAnswer) throws WdkModelException {
    return TransformUtil.transformToNewResultTypeAnswer(
        transcriptAnswer,
        TRANSCRIPT_RECORDCLASS,
        XFORM_QUESTION_NAME,
        XFORM_STEP_ID_PARAM_NAME,
        GENE_RECORDCLASS);
  }

  public static RecordClass getGeneRecordClass(WdkModel wdkModel) {
    return wdkModel.getRecordClassByFullName(GENE_RECORDCLASS)
      .orElseThrow(() -> new WdkRuntimeException(GENE_RECORDCLASS + " does not exist in this model."));
  }

  public static RecordClass getTranscriptRecordClass(WdkModel wdkModel) {
    return wdkModel.getRecordClassByFullName(TRANSCRIPT_RECORDCLASS)
      .orElseThrow(() -> new WdkRuntimeException(TRANSCRIPT_RECORDCLASS + " does not exist in this model."));
  }
}
