package org.apidb.apicommon.test;

import static java.util.Arrays.asList;
import static org.apidb.apicommon.model.TranscriptUtil.isTranscriptQuestion;
import static org.apidb.apicommon.model.TranscriptUtil.transformToGeneAnswer;
import static org.gusdb.fgputil.FormatUtil.TAB;
import static org.gusdb.fgputil.FormatUtil.join;
import static org.gusdb.fgputil.functional.Functions.mapToList;
import static org.gusdb.fgputil.runtime.GusHome.getGusHome;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Map;

import org.gusdb.fgputil.MapBuilder;
import org.gusdb.fgputil.functional.FunctionalInterfaces.Function;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.answer.stream.RecordStream;
import org.gusdb.wdk.model.answer.stream.SingleTableRecordStream;
import org.gusdb.wdk.model.filter.FilterOptionList;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.record.TableField;
import org.gusdb.wdk.model.record.attribute.AttributeField;
import org.gusdb.wdk.model.record.attribute.AttributeValue;
import org.gusdb.wdk.model.user.GuestUser;
import org.gusdb.wdk.model.user.Step;
import org.json.JSONObject;

/** Testing "GeneTranscripts" table using the following step:
{
  "questionName": "GeneQuestions.GenesByExonCount",
  "parameters": {
    "organism": "Plasmodium berghei,Plasmodium berghei ANKA,Plasmodium chabaudi,Plasmodium chabaudi chabaudi,Plasmodium cynomolgi,Plasmodium cynomolgi strain B,Plasmodium falciparum 3D7,Plasmodium gallinaceum,Plasmodium gallinaceum 8A,Plasmodium knowlesi,Plasmodium knowlesi strain H,Plasmodium reichenowi,Plasmodium reichenowi CDC,Plasmodium vivax Sal-1,Plasmodium yoelii yoelii 17X",
    "num_exons_gte": "12",
    "num_exons_lte": "20",
    "scope": "Gene"
  },
  "filters": [{
    "name": "matched_transcript_filter_array",
    "value": {"values": ["Y"]},
    "disabled": false
  }]
}
 */
public class SingleTableRecordStreamTest {

  /*%%%%%%%%%%%%%%%%%%%%%%%%%%% set up our model and request %%%%%%%%%%%%%%%%%%%%%%%%%%%*/

  private static final String PROJECT_ID = "PlasmoDB";

  private static final String QUESTION_NAME = "GeneQuestions.GenesByExonCount";

  private static final Map<String,String> PARAMETERS = new MapBuilder<String,String>()
      .put("organism", "Plasmodium berghei,Plasmodium berghei ANKA,Plasmodium chabaudi," +
          "Plasmodium chabaudi chabaudi,Plasmodium cynomolgi,Plasmodium cynomolgi strain B," +
          "Plasmodium falciparum 3D7,Plasmodium gallinaceum,Plasmodium gallinaceum 8A," +
          "Plasmodium knowlesi,Plasmodium knowlesi strain H,Plasmodium reichenowi," +
          "Plasmodium reichenowi CDC,Plasmodium vivax Sal-1,Plasmodium yoelii yoelii 17X")
      .put("num_exons_gte", "12")
      .put("num_exons_lte", "20")
      .put("scope", "Gene")
      .toMap();

  private static final FilterOptionList FILTERS(WdkModel model) throws WdkModelException {
    return new FilterOptionList(model, QUESTION_NAME)
        .addFilterOption("matched_transcript_filter_array", new JSONObject("{\"values\": [\"Y\"]}")); }

  private static final String TABLE_NAME = "GeneTranscripts";

  /*%%%%%%%%%%%%%%%%%%%%%%%%%%% main method %%%%%%%%%%%%%%%%%%%%%%%%%%%*/

  public static void main(String[] args) throws Exception {
    try (WdkModel model = WdkModel.construct(PROJECT_ID, getGusHome());
         PrintStream out = (args.length == 0 ? System.out : new PrintStream(new FileOutputStream(args[0])))) {
      Step step = createStep(model);
      AnswerValue answer = (isTranscriptQuestion(step.getQuestion()) ?
          transformToGeneAnswer(step.getAnswerValue(), step.getStepId()) : step.getAnswerValue())
          .cloneWithNewPaging(0, -1); // want full results
      TableField tableField = answer.getQuestion().getRecordClass().getTableFieldMap().get(TABLE_NAME);

      try (RecordStream recordStream = new SingleTableRecordStream(answer, tableField)) {
        writeFields(out, tableField, getFieldHeader);
        for (RecordInstance record : recordStream) {
          for (Map<String, AttributeValue> row : record.getTableValue(TABLE_NAME)) {
            writeFields(out, tableField, getFieldValue(row));
          }
        }
      }
    }
  }

  /*%%%%%%%%%%%%%%%%%%%%%%%%%%% helper functions %%%%%%%%%%%%%%%%%%%%%%%%%%%*/

  private static Step createStep(WdkModel model) throws WdkModelException, WdkUserException {
    return model.getStepFactory().createStep(new GuestUser(model), -1L,
        model.getQuestion(QUESTION_NAME), PARAMETERS, null, 0, -1, false, true, 0, FILTERS(model));
  }

  private static Function<AttributeField, String> getFieldValue(final Map<String, AttributeValue> row) {
    return new Function<AttributeField,String>() {
      @Override public String apply(AttributeField field) {
        try { return (String)row.get(field.getName()).getValue(); }
        catch (Exception e) { return "ERR"; }
      }
    };
  }

  private static Function<AttributeField,String> getFieldHeader = new Function<AttributeField,String>(){
    @Override public String apply(AttributeField field) {
      return field.getName();
    }
  };

  private static void writeFields(PrintStream out, TableField tableField, Function<AttributeField, String> function) {
    out.println(join(mapToList(asList(tableField.getAttributeFields()), function).toArray(), TAB));
  }
}
