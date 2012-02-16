/**
 * 
 */
package org.apidb.apicommon.model.view;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
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
public abstract class GenomeViewHandler implements SummaryViewHandler {

    private static final String PROP_SEQUENCES = "sequences";
    private static final String PROP_MAX_LENGTH = "maxLength";

    private static final Logger logger = Logger.getLogger(GenomeViewHandler.class);

    private final String attrSourceId;
    private final String attrSequenceId;
    private final String attrSequenceLength;
    private final String attrStart;
    private final String attrEnd;
    private final String attrStrand;

    public GenomeViewHandler(String attrSourceId, String attrSequenceId,
            String attrSequenceLength, String attrStart, String attrEnd,
            String attrStrand) {
        this.attrSourceId = attrSourceId;
        this.attrSequenceId = attrSequenceId;
        this.attrSequenceLength = attrSequenceLength;
        this.attrStart = attrStart;
        this.attrEnd = attrEnd;
        this.attrStrand = attrStrand;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.gusdb.wdk.view.SummaryViewHandler#process(org.gusdb.wdk.model.user
     * .Step)
     */
    public Map<String, Object> process(Step step) throws WdkModelException,
            WdkUserException {
        logger.debug("Entering SpanGenomeViewHandler...");
        Map<String, Sequence> sequences = new HashMap<String, Sequence>();
        try {
            int maxLength = 0;
            AnswerValue answerValue = step.getAnswerValue();
            for (AnswerValue answer : answerValue.getFullAnswers()) {
                for (RecordInstance recordInstance : answer.getRecordInstances()) {
                    String sourceId = (String) recordInstance.getAttributeValue(
                            attrSourceId).getValue();
                    String sequenceId = (String) recordInstance.getAttributeValue(
                            attrSequenceId).getValue();
                    int length = Integer.valueOf((String) recordInstance.getAttributeValue(
                            attrSequenceLength).getValue());
                    int start = Integer.valueOf((String) recordInstance.getAttributeValue(
                            attrStart).getValue());
                    int end = Integer.valueOf((String) recordInstance.getAttributeValue(
                            attrEnd).getValue());
                    boolean strand = (recordInstance.getAttributeValue(
                            attrStrand).getValue().toString().equals("+"));

                    Span span = new Span(sourceId);
                    span.setSequenceId(sequenceId);
                    span.setStart(start);
                    span.setEnd(end);
                    span.setForward(strand);

                    Sequence sequence = sequences.get(sequenceId);
                    if (sequence == null) {
                        sequence = new Sequence(sequenceId);
                        sequence.setLength(length);
                        sequences.put(sequenceId, sequence);
                    }
                    sequence.addSpan(span);

                    if (maxLength < length) maxLength = length;
                }
            }
            // compute sizes for the sequences & spans
            computeSizes(sequences, maxLength);

            // sort sequences by source ids
            String[] sequenceIds = sequences.keySet().toArray(new String[0]);
            Arrays.sort(sequenceIds);
            Sequence[] array = new Sequence[sequenceIds.length];
            for (int i = 0; i < sequenceIds.length; i++) {
                array[i] = sequences.get(sequenceIds[i]);
            }

            Map<String, Object> results = new HashMap<String, Object>();
            results.put(PROP_SEQUENCES, array);
            results.put(PROP_MAX_LENGTH, maxLength);
            logger.debug("Leaving SpanGenomeViewHandler...");
            return results;
        } catch (NoSuchAlgorithmException ex) {
            logger.error(ex);
            ex.printStackTrace();
            throw new WdkModelException(ex);
        } catch (JSONException ex) {
            logger.error(ex);
            ex.printStackTrace();
            throw new WdkModelException(ex);
        } catch (SQLException ex) {
            logger.error(ex);
            ex.printStackTrace();
            throw new WdkModelException(ex);
        }
    }

    private void computeSizes(Map<String, Sequence> sequences, int maxLength) {
        for (Sequence sequence : sequences.values()) {
            int sequenceLength = sequence.getLength();
            float pctLength = round(sequenceLength * 100F / maxLength);
            sequence.setPercentLength(pctLength);
            
            // the percent length of span is relative to the local sequence
            for (Span span : sequence.getSpans()) {
                float pctStart = round (span.getStart() * 100F / sequenceLength);
                int length = Math.abs(span.getEnd() - span.getStart() + 1);
                pctLength = round(length * 100F / sequenceLength);
                span.setPercentStart(pctStart);
                span.setPercentLength(pctLength);
            }
        }
    }

    private float round(float value) {
        return Math.round(value * 1000) / 1000F;
    }
}
