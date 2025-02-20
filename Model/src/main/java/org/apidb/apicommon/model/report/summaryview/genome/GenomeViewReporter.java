package org.apidb.apicommon.model.report.summaryview.genome;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.db.SqlUtils;
import org.gusdb.fgputil.json.JsonWriter;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.report.AbstractReporter;
import org.gusdb.wdk.model.report.Reporter;
import org.gusdb.wdk.model.report.ReporterConfigException;
import org.json.JSONObject;
import org.json.JSONWriter;

/**
 * @author jerric
 * 
 */
public abstract class GenomeViewReporter extends AbstractReporter {

  public static final DecimalFormat FORMAT = new DecimalFormat("#,###");

  protected static final String COLUMN_START = "start_min";
  protected static final String COLUMN_END = "end_max";
  protected static final String COLUMN_SOURCE_ID = "source_id";
  protected static final String COLUMN_SEQUENCE_ID = "sequence_id";
  protected static final String COLUMN_CHROMOSOME = "chromosome";
  protected static final String COLUMN_ORGANISM = "organism";
  protected static final String COLUMN_SEQUENCE_LENGTH = "sequence_length";
  protected static final String COLUMN_CONTEXT = "context";
  protected static final String COLUMN_STRAND = "strand";
  protected static final String COLUMN_DESCRIPTION = "description";

  private static final String PROP_SEQUENCES = "sequences";
  private static final String PROP_MAX_LENGTH = "maxLength";
  private static final String PROP_IS_DETAIL = "isDetail";

  private static final String PROP_IS_TRUNCATE = "isTruncate";

  // private static final double MAX_REGION_PERCENT_LENGTH = 0.1;
  private static final double MIN_FEATURE_PERCENT_GAP = 0.004;

  private static final long MAX_FEATURES = 10000;

  private static final Logger logger = Logger.getLogger(GenomeViewReporter.class);

  public abstract String prepareSql(String idSql) throws WdkModelException, WdkUserException;

  public static double round(double value) {
    return Math.round(value * 1000) / 1000D;
  }
  
  @Override
  protected void write(OutputStream out) throws WdkModelException {
    try (JsonWriter writer = new JsonWriter(out)) {
      writeJson(_baseAnswer, writer);
    } catch (IOException e) {
      throw new WdkModelException("Unable to write reporter result to output stream", e);
    }

  }

  @Override
  public Reporter configure(JSONObject config) throws ReporterConfigException, WdkModelException {
    return this;
  }


  /*
   * (non-Javadoc)
   * 
   * @see org.gusdb.wdk.view.SummaryViewHandler#process(org.gusdb.wdk.model.user .Step)
   */

  private void writeJson(AnswerValue answerValue, JSONWriter writer) throws WdkModelException {
    logger.debug("Entering GenomeViewReporter...");

    writer.object();

    try {
      // don't render
      if (answerValue.getResultSizeFactory().getResultSize() > MAX_FEATURES) {
        writer.key(PROP_IS_TRUNCATE).value(true);
        writer.endObject();
        return;
      }

      // load sequences
      Sequence[] sequences;
      ResultSet resultSet = null;
      try {
        DataSource dataSource = answerValue.getWdkModel().getAppDb().getDataSource();

        // compose an sql to get all sequences from the feature id query.
        String idSql = answerValue.getIdSql();
        String sql = prepareSql(idSql);
        resultSet = SqlUtils.executeQuery(dataSource, sql, "genome-view", 2000);

        sequences = loadSequences(dataSource, sql, resultSet);
      }
      catch (SQLException ex) {
        logger.error(ex);
        ex.printStackTrace();
        throw new WdkModelException(ex);
      }
      finally {
        SqlUtils.closeResultSetAndStatement(resultSet, null);
      }

      // check if want to display the detail view, or density view
      int maxFeatures = 0;
      for (Sequence sequence : sequences) {
        int featureCount = sequence.getFeatureCount();
        if (maxFeatures < featureCount)
          maxFeatures = featureCount;
      }

      // get the max length, then format sequences
      int regionCount = 0;
      long maxLength = getMaxLength(sequences);
      for (Sequence sequence : sequences) {
        // compute the percent length of a sequence.
        double pctLength = round(sequence.getLength() * 100D / maxLength);
        sequence.setPercentLength(pctLength);

        createRegions(sequence, maxLength);
        regionCount += sequence.getRegionCount();
      }
      logger.debug("# regions: " + regionCount);

      // only use detail view for now
      writer.key(PROP_IS_DETAIL).value(true);

      writer.key(PROP_SEQUENCES).array();
      for (Sequence sequence : sequences) {
        sequence.writeJson(writer);
      }
      writer.endArray();

      writer.key(PROP_MAX_LENGTH).value(maxLength);
      logger.debug("Leaving GenomeViewReporter...");
      writer.endObject();
    }
    catch (WdkUserException e) {
      throw new WdkModelException(e);
    }

  }

  private Sequence[] loadSequences(DataSource dataSource, String sql, ResultSet resultSet) throws SQLException {
    // first load all chromosomes
    Map<String, Sequence> sequences = loadChromosomes(dataSource, sql);

    // then load sequences
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
    Sequence[] array = sequences.values().toArray(new Sequence[0]);
    Arrays.sort(array);

    return array;
  }

  private Sequence createSequence(String sequenceId, ResultSet resultSet) throws SQLException {
    Sequence sequence = new Sequence(sequenceId);

    sequence.setLength(resultSet.getInt(COLUMN_SEQUENCE_LENGTH));
    sequence.setChromosome(resultSet.getString(COLUMN_CHROMOSOME));
    sequence.setOrganism(resultSet.getString(COLUMN_ORGANISM));

    return sequence;
  }

  private Feature createFeature(String sequenceId, ResultSet resultSet) throws SQLException {
    String featureId = resultSet.getString(COLUMN_SOURCE_ID);
    boolean forward = resultSet.getBoolean(COLUMN_STRAND);
    Feature feature = new Feature(featureId, sequenceId, forward);

    feature.setStart(resultSet.getInt(COLUMN_START));
    feature.setEnd(resultSet.getInt(COLUMN_END));
    feature.setContext(resultSet.getString(COLUMN_CONTEXT));
    feature.setDescription(resultSet.getString(COLUMN_DESCRIPTION));

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

  private void createRegions(Sequence sequence, long maxLength) throws GenomeViewException {
    // sort features by location
    List<Feature> features = sequence.getFeatures();
    Collections.sort(features);

    // create regions
    Region forwardRegion = null, reversedRegion = null;
    for (Feature feature : sequence.getFeatures()) {
      Region region = feature.isForward() ? forwardRegion : reversedRegion;

      // check if we need to create new region;
      if (region == null || ((feature.getStart() - region.getEnd()) / (double)maxLength > MIN_FEATURE_PERCENT_GAP) 
          /* || (region.getLength() / (double)maxLength > MAX_REGION_PERCENT_LENGTH) */) {
        // region is null, which is the first feature; or region is too far from the next feature; or region
        // is too long already.
        region = new Region(feature.isForward());
        sequence.addRegion(region);

        // set the region back to the proper variable for future use.
        if (feature.isForward())
          forwardRegion = region;
        else
          reversedRegion = region;
      }
      region.addFeature(feature);
    }

    // compute percent size for regions
    for (Region region : sequence.getRegions()) {
      region.computePercentSize(maxLength);
    }
  }

  private Map<String, Sequence> loadChromosomes(DataSource dataSource, String sql) throws SQLException {
    Map<String, Sequence> chromosomes = new HashMap<>();
    sql = "SELECT source_id AS " + COLUMN_SEQUENCE_ID + ", length AS " + COLUMN_SEQUENCE_LENGTH 
        + ", chromosome AS " + COLUMN_CHROMOSOME + ", organism AS " + COLUMN_ORGANISM 
        + " FROM ApidbTuning.GenomicSeqAttributes "
        + " WHERE chromosome IS NOT NULL "
        + "   AND organism IN (SELECT organism FROM (" + sql + ") t )";
    ResultSet resultSet = null;
    try {
      resultSet = SqlUtils.executeQuery(dataSource, sql, "genome-view-chromosome");
      while(resultSet.next()) {
        String sequenceId = resultSet.getString(COLUMN_SEQUENCE_ID);
        Sequence chromosome = createSequence(sequenceId, resultSet);
        chromosomes.put(sequenceId, chromosome);
      }
    } finally {
      SqlUtils.closeResultSetAndStatement(resultSet, null);
    }
    return chromosomes;
  }
  
  @Override
  public Reporter configure(Map<String, String> config) throws ReporterConfigException, WdkModelException {
    return this;
  }

}
