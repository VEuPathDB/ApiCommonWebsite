/**
 * 
 */
package org.apidb.apicommon.model.view;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.gusdb.wdk.model.AnswerValue;
import org.gusdb.wdk.model.RecordInstance;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.view.SummaryViewHandler;
import org.json.JSONException;

/**
 * @author jerric
 * 
 */
public class SpanGenomeViewHandler implements SummaryViewHandler {

    private static final String ATTRIBUTE_START = "start_min";
    private static final String ATTRIBUTE_END = "end_max";
    private static final String ATTRIBUTE_SOURCE_ID = "source_id";
    private static final String ATTRIBUTE_SEQUENCE_SOURCE_ID = "seq_source_id";
    private static final String ATTRIBUTE_SEQUENCE_LENGTH = "sequence_length";

    private static final String PROP_SEQUENCES = "sequences";
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.gusdb.wdk.view.SummaryViewHandler#process(org.gusdb.wdk.model.user
     * .Step)
     */
    public Map<String, Object> process(Step step) throws WdkModelException,
            WdkUserException {
        Map<String, Sequence> sequences = new HashMap<String, Sequence>();
        try {
            AnswerValue answerValue = step.getAnswerValue();
            for (AnswerValue answer : answerValue.getFullAnswers()) {
                for (RecordInstance recordInstance : answer.getRecordInstances()) {
                    String sourceId = (String) recordInstance.getAttributeValue(
                            ATTRIBUTE_SOURCE_ID).getValue();
                    String sequenceId = (String) recordInstance.getAttributeValue(
                            ATTRIBUTE_SEQUENCE_SOURCE_ID).getValue();
                    int length = (Integer) recordInstance.getAttributeValue(
                            ATTRIBUTE_SEQUENCE_LENGTH).getValue();
                    int start = (Integer) recordInstance.getAttributeValue(
                            ATTRIBUTE_START).getValue();
                    int end = (Integer) recordInstance.getAttributeValue(
                            ATTRIBUTE_END).getValue();

                    DynamicSpan span = new DynamicSpan(sourceId);
                    span.setSequenceId(sequenceId);
                    span.setStart(start);
                    span.setEnd(end);

                    Sequence sequence = sequences.get(sequenceId);
                    if (sequence == null) {
                        sequence = new Sequence(sequenceId);
                        sequence.setLength(length);
                        sequences.put(sequenceId, sequence);
                    }
                    sequence.addSpan(span);
                }
            }
            
            // sort sequences by source ids
            String[] sequenceIds = sequences.keySet().toArray(new String[0]);
            Arrays.sort(sequenceIds);
            Sequence[] array = new Sequence[sequenceIds.length];
            for (int i = 0; i < sequenceIds.length; i++) {
                array[i] = sequences.get(sequenceIds[i]);
            }

            Map<String, Object> results  = new HashMap<String, Object>();
            results.put(PROP_SEQUENCES, array);
            return results;
        } catch (NoSuchAlgorithmException ex) {
            throw new WdkModelException(ex);
        } catch (JSONException ex) {
            throw new WdkModelException(ex);
        } catch (SQLException ex) {
            throw new WdkModelException(ex);
        }

    }

}
