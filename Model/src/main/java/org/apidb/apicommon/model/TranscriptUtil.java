package org.apidb.apicommon.model;

import java.util.Arrays;

import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.answer.TransformUtil;
import org.gusdb.wdk.model.question.Question;
import org.gusdb.wdk.model.record.RecordClass;

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

  public static boolean isProjectIdInPks(WdkModel wdkModel) {
    boolean transcriptPkHasProjectId = isProjectInPk(getTranscriptRecordClass(wdkModel));
    boolean genePkHasProjectId = isProjectInPk(getGeneRecordClass(wdkModel));
    if (transcriptPkHasProjectId != genePkHasProjectId) {
      throw new WdkRuntimeException("One of [ gene, transcript ] record class primary key defs has " +
          "project_id and one does not.  This will break many gene/transcript-specific logic coding.");
    }
    return transcriptPkHasProjectId;
  }

  private static boolean isProjectInPk(RecordClass recordClass) {
    return Arrays.asList(recordClass.getPrimaryKeyDefinition().getColumnRefs()).contains("project_id");
  }

}
