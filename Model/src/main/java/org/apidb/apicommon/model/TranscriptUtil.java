package org.apidb.apicommon.model;

import java.util.Map;

import org.gusdb.fgputil.MapBuilder;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.answer.factory.AnswerValueFactory;
import org.gusdb.wdk.model.answer.spec.AnswerSpec;
import org.gusdb.wdk.model.question.Question;
import org.gusdb.wdk.model.record.RecordClass;

public class TranscriptUtil {

  public static final String GENE_RECORDCLASS = "GeneRecordClasses.GeneRecordClass";
  public static final String TRANSCRIPT_RECORDCLASS = "TranscriptRecordClasses.TranscriptRecordClass";

  private static final String XFORM_QUESTION_NAME = "GeneRecordQuestions.GenesFromTranscripts";

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

  public static AnswerValue transformToGeneAnswer(AnswerValue transcriptAnswer, long stepId) throws WdkUserException {
    try {
      Question question = transcriptAnswer.getAnswerSpec().getQuestion().getWdkModel().getQuestion(XFORM_QUESTION_NAME);
      if (question == null) {
        throw new WdkModelException("Can't find xform with name: " + XFORM_QUESTION_NAME);
      }
      String paramName = "gene_result";
      if (question.getParamMap().size() != 1 || !question.getParamMap().containsKey(paramName)) {
        throw new WdkModelException("Expected question " + XFORM_QUESTION_NAME +
            " to have exactly one parameter named " + paramName);
      }
      Map<String, String> params = new MapBuilder<String, String>(paramName, String.valueOf(stepId)).toMap();
      AnswerValue geneAnswer = AnswerValueFactory.makeAnswer(transcriptAnswer.getUser(),
          AnswerSpec.builder(question.getWdkModel())
                    .setQuestionName(XFORM_QUESTION_NAME)
                    .setParamValues(params)
                    .setAssignedWeight(10)
                    .buildRunnable());

      // make sure gene answer uses same page size as transcript answer
      return geneAnswer.cloneWithNewPaging(transcriptAnswer.getStartIndex(), transcriptAnswer.getEndIndex());
    }
    catch (WdkModelException e) {
      // unfortunately best place to call this is in configure(), which only throws WdkUserException
      throw new WdkUserException(e);
    }
  }
}
