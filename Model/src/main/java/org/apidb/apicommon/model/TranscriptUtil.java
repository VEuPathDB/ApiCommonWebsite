package org.apidb.apicommon.model;

import java.util.Arrays;
import org.apidb.apicommon.model.filter.RepresentativeTranscriptFilter;
import org.gusdb.fgputil.validation.ValidObjectFactory.RunnableObj;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.answer.TransformUtil;
import org.gusdb.wdk.model.answer.factory.AnswerValueFactory;
import org.gusdb.wdk.model.answer.spec.AnswerSpec;
import org.gusdb.wdk.model.answer.spec.FilterOptionList;
import org.gusdb.wdk.model.question.Question;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.model.report.ReporterConfigException;
import org.json.JSONException;
import org.json.JSONObject;

public class TranscriptUtil {

  public static final String GENE_RECORDCLASS = "GeneRecordClasses.GeneRecordClass";
  public static final String TRANSCRIPT_RECORDCLASS = "TranscriptRecordClasses.TranscriptRecordClass";

  private static final String GENE_XFORM_QUESTION_NAME = "GeneRecordQuestions.GenesFromTranscripts";
  private static final String GENE_XFORM_STEP_ID_PARAM_NAME = "gene_result";

  private static final String TRANSCRIPT_XFORM_QUESTION_NAME = "TranscriptRecordQuestions.TranscriptsFromGenes";
  private static final String TRANSCRIPT_XFORM_STEP_ID_PARAM_NAME = "transcript_result";
  
  // used in config API for reporters that want to apply a one-gene-per-transcript filter
  public static final String PROP_APPLY_FILTER = "applyFilter";

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
        GENE_XFORM_QUESTION_NAME,
        GENE_XFORM_STEP_ID_PARAM_NAME,
        GENE_RECORDCLASS);
  }

  public static AnswerValue transformToTranscriptAnswer(AnswerValue geneAnswer) throws WdkModelException {
    return TransformUtil.transformToNewResultTypeAnswer(
        geneAnswer,
        GENE_RECORDCLASS,
        TRANSCRIPT_XFORM_QUESTION_NAME,
        TRANSCRIPT_XFORM_STEP_ID_PARAM_NAME,
        TRANSCRIPT_RECORDCLASS);
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
  
  // return the state of the "one gene per transcript" property
  static public boolean getApplyOneGeneFilterProp(JSONObject config) {
    try {
      return config.getBoolean(TranscriptUtil.PROP_APPLY_FILTER);
    }
    catch (JSONException e) {
      return false;  // default to false if prop not present
    }    
  }

  // apply the one gene per transcript filter.
  public static AnswerValue getOneGenePerTranscriptAnswerValue(AnswerValue baseAnswer) throws WdkModelException {
    Question question = baseAnswer.getAnswerSpec().getQuestion();
    String filterName = RepresentativeTranscriptFilter.FILTER_NAME;
    if (question.getFilter(filterName) == null) {
      throw new WdkModelException("Can't find transcript filter with name " +
          filterName + " on question " + question.getFullName());
    }
    JSONObject jsFilterValue = new JSONObject(); // this filter has no params, so this stays empty
    RunnableObj<AnswerSpec> modifiedSpec = AnswerSpec
        .builder(baseAnswer.getAnswerSpec())
        .setViewFilterOptions(FilterOptionList.builder().addFilterOption(filterName, jsFilterValue))
        .buildRunnable(baseAnswer.getUser(), baseAnswer.getAnswerSpec().getStepContainer());
    return AnswerValueFactory.makeAnswer(baseAnswer, modifiedSpec);
  }


  private static boolean isProjectInPk(RecordClass recordClass) {
    return Arrays.asList(recordClass.getPrimaryKeyDefinition().getColumnRefs()).contains("project_id");
  }

}
