/**
 * 
 */
package org.apidb.apicommon.model.view.genome;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
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
  protected static final String COLUMN_ORGANISM = "organism";
  protected static final String COLUMN_SEQUENCE_LENGTH = "sequence_length";
  protected static final String COLUMN_CONTEXT = "context";
  protected static final String COLUMN_STRAND = "strand";

  private static final String PROP_SEQUENCES = "sequences";
  private static final String PROP_MAX_LENGTH = "maxLength";

  private static final int DETAIL_LIMIT = 1000;

  private static final double SEGMENTS = 20;

  private static final Logger logger = Logger.getLogger(GenomeViewHandler.class);

  public abstract String prepareSql(String idSql) throws WdkModelException,
      WdkUserException;

  /*
   * (non-Javadoc)
   * 
   * @see org.gusdb.wdk.view.SummaryViewHandler#process(org.gusdb.wdk.model.user
   * .Step)
   */
  public Map<String, Object> process(Step step) throws WdkModelException,
      WdkUserException {
    logger.debug("Entering SpanGenomeViewHandler...");

    // load sequences
    Sequence[] sequences;
    ResultSet resultSet = null;
    try {
      WdkModel wdkModel = step.getQuestion().getWdkModel();
      DataSource dataSource = wdkModel.getQueryPlatform().getDataSource();

      // compose an sql to get all sequences from the feature id query.
      AnswerValue answerValue = step.getAnswerValue();
      String sql = prepareSql(answerValue.getIdSql());
      resultSet = SqlUtils.executeQuery(wdkModel, dataSource, sql,
          "genome-view", 2000);

      sequences = loadSequences(resultSet);
    } catch (SQLException ex) {
      logger.error(ex);
      ex.printStackTrace();
      throw new WdkModelException(ex);
    } finally {
      SqlUtils.closeResultSet(resultSet);
    }

    // get the max length, then format sequences
    long maxLength = getMaxLength(sequences);
    for (Sequence sequence : sequences) {
      // compute the percent length of a sequence.
      double pctLength = round(sequence.getLength() * 100D / maxLength);
      sequence.setPercentLength(pctLength);

      // format features by max length
      formatFeatures(sequence, maxLength);

      // format regions if needed
      if (sequence.getFeatureCount() > DETAIL_LIMIT)
        formatRegions(sequence, maxLength);
    }

    Map<String, Object> results = new HashMap<String, Object>();
    results.put(PROP_SEQUENCES, sequences);
    results.put(PROP_MAX_LENGTH, maxLength);
    logger.debug("Leaving SpanGenomeViewHandler...");
    return results;
  }

  private Sequence[] loadSequences(ResultSet resultSet) throws SQLException {
    Map<String, Sequence> sequences = new HashMap<String, Sequence>();
    while (resultSet.next()) {
      String sequenceId = resultSet.getString(COLUMN_SEQUENCE_ID);
      Sequence sequence = sequences.get(sequenceId);
      if (sequence == null) {
        sequence = createSequence(sequenceId, resultSet);
        sequences.put(sequenceId, sequence);
      }

      Feature feature = createFeature(sequenceId, resultSet);
      sequence.addFeature(feature);
    }
    return sequences.values().toArray(new Sequence[0]);
  }

  private Sequence createSequence(String sequenceId, ResultSet resultSet)
      throws SQLException {
    Sequence sequence = new Sequence(sequenceId);

    sequence.setLength(resultSet.getInt(COLUMN_SEQUENCE_LENGTH));
    sequence.setChromosome(resultSet.getString(COLUMN_CHROMOSOME));
    sequence.setOrganism(resultSet.getString(COLUMN_ORGANISM));

    return sequence;
  }

  private Feature createFeature(String sequenceId, ResultSet resultSet)
      throws SQLException {
    String featureId = resultSet.getString(COLUMN_SOURCE_ID);
    Feature feature = new Feature(featureId);

    feature.setSequenceId(sequenceId);
    feature.setStart(resultSet.getInt(COLUMN_START));
    feature.setEnd(resultSet.getInt(COLUMN_END));
    feature.setForward(resultSet.getBoolean(COLUMN_STRAND));
    feature.setContext(resultSet.getString(COLUMN_CONTEXT));

    return feature;
  }

  private long getMaxLength(Sequence[] sequences) {
    long maxLength = 0;
    for (Sequence sequence : sequences) {
      if (sequence.getLength() > maxLength)
        maxLength = sequence.getLength();
    }
    return maxLength;
  }

  public void formatFeatures(Sequence sequence, long maxLength) {
    for (Feature feature : sequence.getFeatures()) {
      double pctStart = round(feature.getStart() * 100D / maxLength);
      long length = Math.abs(feature.getEnd() - feature.getStart() + 1);
      double pctLength = round(length * 100D / maxLength);
      feature.setPercentStart((float) pctStart);
      feature.setPercentLength(pctLength);
    }
  }

  public void formatRegions(Sequence sequence, long maxLength) {
    long sequenceLength = sequence.getLength();
    long segmentLength = Math.round(maxLength / SEGMENTS);
    long start = 1;
    while (start <= sequenceLength) {
      // determine the start & stop of the current region.
      long stop = start + segmentLength - 1;
      if (stop > sequenceLength
          || (sequenceLength - stop) / (double) segmentLength < 0.1)
        stop = sequenceLength;

      // create two regions at the same section.
      Region forward = new Region(sequence.getSourceId(), start, stop, true);
      Region reverse = new Region(sequence.getSourceId(), start, stop, false);

      double pctStart = round(start * 100D / maxLength);
      double pctLength = round((stop - start + 1) * 100 / maxLength);
      forward.setPercentStart(pctStart);
      forward.setPercentLength(pctLength);
      sequence.addRegion(forward);
      sequence.addRegion(reverse);

      start = stop + 1;
    }

    // assign features to regions
    for (Feature feature : sequence.getFeatures()) {
      List<Region> regions = sequence.getRegions(feature.getStart(),
          feature.getEnd(), feature.isForward());
      for (Region region : regions) {
        region.addFeature(feature);
      }
    }
  }

  private double round(double value) {
    return Math.round(value * 1000) / 1000D;
  }
}
