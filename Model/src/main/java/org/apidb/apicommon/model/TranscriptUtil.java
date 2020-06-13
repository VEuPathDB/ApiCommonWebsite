package org.apidb.apicommon.model;

import java.util.Map;
import java.util.Optional;

import org.gusdb.fgputil.MapBuilder;
import org.gusdb.fgputil.validation.ValidObjectFactory.RunnableObj;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.gusdb.wdk.model.answer.AnswerValue;
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
   * Takes a runnable transcript step and returns an answer spec of a transform
   * that will return the genes of the transcripts returned by the step.
   *
   * @param wdkModel
   * @param user user 
   * @param transcriptStep step that returns transcripts
   * @return answer spec that will return genes
   * @throws WdkModelException if error occurs or caller sends bad args
   */
  private static RunnableObj<AnswerSpec> transformToRunnableGeneAnswerSpec(
      WdkModel wdkModel, User user, RunnableObj<Step> transcriptStep) throws WdkModelException {

    if (!isTranscriptQuestion(transcriptStep.get().getAnswerSpec().getQuestion())) {
      throw new WdkModelException("Step to be transformed to genes must return transcripts");
    }

    Question question = wdkModel.getQuestionByFullName(XFORM_QUESTION_NAME)
        .orElseThrow(() -> new WdkModelException("Can't find xform with name: " + XFORM_QUESTION_NAME));

    if (question.getParamMap().size() != 1 || !question.getParamMap().containsKey(XFORM_STEP_ID_PARAM_NAME)) {
      throw new WdkModelException("Expected question " + XFORM_QUESTION_NAME +
          " to have exactly one parameter named " + XFORM_STEP_ID_PARAM_NAME);
    }

    Map<String, String> transformParams = new MapBuilder<String, String>(
        XFORM_STEP_ID_PARAM_NAME, String.valueOf(transcriptStep.get().getStepId())).toMap();

    return AnswerSpec
        .builder(wdkModel)
        .setQuestionFullName(XFORM_QUESTION_NAME)
        .setQueryInstanceSpec(QueryInstanceSpec.builder()
            .putAll(transformParams)
            .setAssignedWeight(10)
        )
        .buildRunnable(user, new ListStepContainer(transcriptStep.get()));
  }

  public static AnswerValue transformToGeneAnswer(AnswerValue transcriptAnswer) throws WdkModelException {

    WdkModel model = transcriptAnswer.getWdkModel();
    User user = transcriptAnswer.getUser();
    AnswerSpec transcriptSpec = transcriptAnswer.getAnswerSpec();

    Optional<Strategy> strategy = transcriptSpec.getStepContainer() instanceof Strategy ?
        Optional.of((Strategy)transcriptSpec.getStepContainer()) : Optional.empty();

    RunnableObj<Step> step = Step.builder(model, user.getUserId(), model.getStepFactory().getNewStepId())
        .setAnswerSpec(AnswerSpec.builder(transcriptAnswer.getAnswerSpec()))
        .setStrategyId(strategy.map(strat -> strat.getStrategyId()))
        .buildRunnable(new UserCache(user), strategy);

    AnswerValue geneAnswer = AnswerValueFactory.makeAnswer(user,
        transformToRunnableGeneAnswerSpec(model, user, step));

    // make sure gene answer uses same page size as transcript answer
    return geneAnswer.cloneWithNewPaging(transcriptAnswer.getStartIndex(), transcriptAnswer.getEndIndex());
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
