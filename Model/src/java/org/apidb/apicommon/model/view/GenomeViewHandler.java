/**
 * 
 */
package org.apidb.apicommon.model.view;

import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.gusdb.wdk.model.AnswerValue;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.dbms.SqlUtils;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.view.SummaryViewHandler;
import org.json.JSONException;

/**
 * @author jerric
 * 
 */
public abstract class GenomeViewHandler implements SummaryViewHandler {

    protected static final String COLUMN_START = "start_min";
    protected static final String COLUMN_END = "end_max";
    protected static final String COLUMN_SOURCE_ID = "source_id";
    protected static final String COLUMN_SEQUENCE_ID = "sequence_id";
    protected static final String COLUMN_CHROMOSOME = "chromosome";
    protected static final String COLUMN_SEQUENCE_LENGTH = "sequence_length";
    protected static final String COLUMN_STRAND = "strand";

    private static final String PROP_SEQUENCES = "sequences";
    private static final String PROP_MAX_LENGTH = "maxLength";

    private static final Logger logger = Logger.getLogger(GenomeViewHandler.class);

    public abstract String prepareSql(String idSql) throws WdkModelException,
            WdkUserException;

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

        ResultSet resultSet = null;
        try {
            WdkModel wdkModel = step.getQuestion().getWdkModel();
            AnswerValue answerValue = step.getAnswerValue();
            String sql = prepareSql(answerValue.getIdSql());
            DataSource dataSource = wdkModel.getQueryPlatform().getDataSource();
            resultSet = SqlUtils.executeQuery(wdkModel, dataSource, sql,
                    "genome-view", 2000);

            int maxLength = 0;
            Map<String, Sequence> sequences = new HashMap<String, Sequence>();
            while (resultSet.next()) {
                String sequenceId = resultSet.getString(COLUMN_SEQUENCE_ID);
                Sequence sequence = sequences.get(sequenceId);
                if (sequence == null) {
                    sequence = new Sequence(sequenceId);
                    sequences.put(sequenceId, sequence);

                    int length = resultSet.getInt(COLUMN_SEQUENCE_LENGTH);
                    sequence.setLength(length);
                    if (maxLength < length) maxLength = length;
                    sequence.setChromosome(resultSet.getString(COLUMN_CHROMOSOME));
                }

                String spanId = resultSet.getString(COLUMN_SOURCE_ID);
                Span span = new Span(spanId);
                span.setSequenceId(sequenceId);
                span.setStart(resultSet.getInt(COLUMN_START));
                span.setEnd(resultSet.getInt(COLUMN_END));
                span.setForward(resultSet.getBoolean(COLUMN_STRAND));
                sequence.addSpan(span);
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
        } finally {
            SqlUtils.closeResultSet(resultSet);
        }
    }

    private void computeSizes(Map<String, Sequence> sequences, int maxLength) {
        for (Sequence sequence : sequences.values()) {
            int sequenceLength = sequence.getLength();
            float pctLength = round(sequenceLength * 100F / maxLength);
            sequence.setPercentLength(pctLength);

            // the percent length of span is relative to the local sequence
            for (Span span : sequence.getSpans()) {
                float pctStart = round(span.getStart() * 100F / sequenceLength);
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
