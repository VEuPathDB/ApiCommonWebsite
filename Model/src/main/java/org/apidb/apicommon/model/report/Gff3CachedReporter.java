package org.apidb.apicommon.model.report;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.db.SqlUtils;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.stream.RecordStream;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.record.attribute.AttributeValue;
import org.gusdb.wdk.model.report.PropertiesProvider;
import org.gusdb.wdk.model.report.ReporterConfigException;
import org.gusdb.wdk.model.report.config.StandardConfig;
import org.gusdb.wdk.model.report.reporter.PagedAnswerReporter;
import org.json.JSONObject;

/**
 * Used exclusively on the website that provides result download in GFF format. It will use the data from
 * apidb.GeneDetails table.
 * 
 * @author xingao
 * 
 */
public class Gff3CachedReporter extends PagedAnswerReporter {

  private static Logger logger = Logger.getLogger(Gff3CachedReporter.class);

  private static final String[] SUPPORTED_RECORD_CLASS_NAMES = {
      "GeneRecordClasses.GeneRecordClass",
      "TranscriptRecordClasses.TranscriptRecordClass"
  };

  public static final String PROPERTY_TABLE_CACHE = "table_cache";
  public static final String PROPERTY_RECORD_ID_COLUMN = "record_id_column";

  public static final String PROPERTY_GFF_RECORD_NAME = "gff_record";
  public static final String PROPERTY_GFF_TRANSCRIPT_NAME = "gff_transcript";
  public static final String PROPERTY_GFF_PROTEIN_NAME = "gff_protein";

  public final static String FIELD_HAS_TRANSCRIPT = "hasTranscript";
  public final static String FIELD_HAS_PROTEIN = "hasProtein";

  private final static String COLUMN_CONTENT = "content";

  private String tableCache;
  private String recordIdColumn;
  private String recordName;
  private String proteinName;
  private String transcriptName;

  private boolean hasTranscript = false;
  private boolean hasProtein = false;
  private String fileType = null;

  @Override
  public Gff3CachedReporter setProperties(PropertiesProvider reporterRef) throws WdkModelException {
    super.setProperties(reporterRef);

    // check required properties
    tableCache = _properties.get(PROPERTY_TABLE_CACHE);
    recordIdColumn = _properties.get(PROPERTY_RECORD_ID_COLUMN);
    recordName = _properties.get(PROPERTY_GFF_RECORD_NAME);
    proteinName = _properties.get(PROPERTY_GFF_PROTEIN_NAME);
    transcriptName = _properties.get(PROPERTY_GFF_TRANSCRIPT_NAME);

    if (tableCache == null || tableCache.length() == 0)
      throw new WdkModelException("The required property for reporter " + this.getClass().getName() + ", " +
          PROPERTY_TABLE_CACHE + ", is missing");
    if (recordIdColumn == null || recordIdColumn.length() == 0)
      throw new WdkModelException("The required property for reporter " + this.getClass().getName() + ", " +
          PROPERTY_RECORD_ID_COLUMN + ", is missing");
    if (recordName == null || recordName.length() == 0)
      throw new WdkModelException("The required property for reporter " + this.getClass().getName() + ", " +
          PROPERTY_GFF_RECORD_NAME + ", is missing");
    if (proteinName == null || proteinName.length() == 0)
      throw new WdkModelException("The required property for reporter " + this.getClass().getName() + ", " +
          PROPERTY_GFF_PROTEIN_NAME + ", is missing");
    if (transcriptName == null || transcriptName.length() == 0)
      throw new WdkModelException("The required property for reporter " + this.getClass().getName() + ", " +
          PROPERTY_GFF_PROTEIN_NAME + ", is missing");

    return this;
  }

  @Override
  public Gff3CachedReporter configure(Map<String, String> config) {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public Gff3CachedReporter configure(JSONObject newConfig) throws ReporterConfigException {

    if (newConfig.has(StandardConfig.ATTACHMENT_TYPE_JSON))
      fileType = newConfig.getString(StandardConfig.ATTACHMENT_TYPE_JSON);

    // include transcript
    if (newConfig.has(FIELD_HAS_TRANSCRIPT))
      hasTranscript = newConfig.getBoolean(FIELD_HAS_TRANSCRIPT);

    // include protein
    if (newConfig.has(FIELD_HAS_PROTEIN))
      hasProtein = newConfig.getBoolean(FIELD_HAS_PROTEIN);

    return this;
  }

  @Override
  public String getDownloadFileName() {
    logger.info("Internal format: " + fileType);
    String name = getQuestion().getName();
    if ("text".equalsIgnoreCase(fileType)) {
      return name + ".gff";
    }
    else { // use the default file name defined in the parent
      return super.getDownloadFileName();
    }
  }

  @Override
  public void write(OutputStream out) throws WdkModelException {
    try {
      PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
  
      // this reporter only works for GeneRecordClasses.GeneRecordClass
      String rcName = getQuestion().getRecordClass().getFullName();
      if (!Arrays.asList(SUPPORTED_RECORD_CLASS_NAMES).contains(rcName))
        throw new WdkModelException("Unsupported record type: " + rcName);
  
      // write header
      writeHeader(writer);
  
      // write record
      writeRecords(writer);
  
      // write sequence
      if (hasTranscript || hasProtein) {
        writeSequences(writer);
      }
    }
    catch (NumberFormatException | WdkUserException e) {
      throw new WdkModelException("Unable to write Gff3 Cached report", e);
    }
  }

  private void writeHeader(PrintWriter writer) throws WdkModelException, NumberFormatException,
      WdkUserException {
    writer.println("##gff-version 3");
    writer.println("# feature-ontology so.obo");
    writer.println("# attribute-ontology gff3_attributes.obo");
    writer.flush();

    // get the sequence regions
    Map<String, int[]> regions = new LinkedHashMap<String, int[]>();

    try (RecordStream records = getRecords()) {
      for (RecordInstance record : records) {
        String seqId = getValue(record.getAttributeValue("gff_seqid"));
        int start = Integer.parseInt(getValue(record.getAttributeValue("gff_fstart")));
        int stop = Integer.parseInt(getValue(record.getAttributeValue("gff_fend")));
        if (regions.containsKey(seqId)) {
          int[] region = regions.get(seqId);
          if (region[0] > start)
            region[0] = start;
          if (region[1] < stop)
            region[1] = stop;
          regions.put(seqId, region);
        }
        else {
          int[] region = { start, stop };
          regions.put(seqId, region);
        }
      }
    }

    // put sequence id into the header
    for (String seqId : regions.keySet()) {
      int[] region = regions.get(seqId);
      writer.println("##sequence-region " + seqId + " " + region[0] + " " + region[1]);
    }
    writer.flush();
  }

  private void writeRecords(PrintWriter writer) throws WdkModelException {
    // get primary key columns
    //RecordClass recordClass = getQuestion().getRecordClass();
    //String[] pkColumns = recordClass.getPrimaryKeyAttributeField().getColumnRefs();

    // get id sql, then combine with the gff cache.
    String idSql = _baseAnswer.getSortedIdSql();


    String sourceIdField = "source_id";
    if(getQuestion().getRecordClass().getFullName().equals("TranscriptRecordClasses.TranscriptRecordClass")) {
        sourceIdField = "gene_source_id";
    }

    StringBuffer sql = new StringBuffer("SELECT tc." + COLUMN_CONTENT);
    sql.append(" FROM " + tableCache + " tC WHERE tc.source_id in (select " + sourceIdField + " from (" + idSql + ") ac)");
    sql.append(" AND tc.table_name = '" + recordName + "' ");


    DatabaseInstance db = getQuestion().getWdkModel().getAppDb();

    // get the result from database
    ResultSet rsTable = null;
    try {
      rsTable = SqlUtils.executeQuery(db.getDataSource(), sql.toString(), "api-report-gff-select-content");

      while (rsTable.next()) {
        String content = db.getPlatform().getClobData(rsTable, "content");
        writer.print(content);
        writer.flush();
      }
    }
    catch (SQLException ex) {
      throw new WdkModelException(ex);
    }
    finally {
      SqlUtils.closeResultSetAndStatement(rsTable, null);
    }
  }

  private void writeSequences(PrintWriter writer) throws WdkModelException {
    // get primary key columns
    //RecordClass recordClass = getQuestion().getRecordClass();
    //String[] pkColumns = recordClass.getPrimaryKeyAttributeField().getColumnRefs();

    // get id sql
    String idSql = _baseAnswer.getSortedIdSql();

    // construct in clause
    StringBuffer sqlIn = new StringBuffer();
    if (hasTranscript)
      sqlIn.append("'" + transcriptName + "'");
    if (hasProtein) {
      if (sqlIn.length() > 0)
        sqlIn.append(", ");
      sqlIn.append("'" + proteinName + "'");
    }



    String sourceIdField = "source_id";
    if(getQuestion().getRecordClass().getFullName().equals("TranscriptRecordClasses.TranscriptRecordClass")) {
        sourceIdField = "gene_source_id";
    }

    StringBuffer sql = new StringBuffer("SELECT ");
    sql.append("tc.").append(COLUMN_CONTENT).append(" FROM ");
    sql.append(tableCache).append(" tc where tc.source_id in (select " + sourceIdField + " from (").append(idSql).append(") aC)");

    DatabaseInstance db = getQuestion().getWdkModel().getAppDb();

    writer.println("##FASTA");

    // get the result from database
    ResultSet rsTable = null;
    try {
      rsTable = SqlUtils.executeQuery(db.getDataSource(), sql.toString(), "api-report-gff-select-content");

      while (rsTable.next()) {
        String content = db.getPlatform().getClobData(rsTable, "content");
        writer.print(content);
        writer.flush();
      }
    }
    catch (SQLException ex) {
      throw new WdkModelException(ex);
    }
    finally {
      SqlUtils.closeResultSetAndStatement(rsTable, null);
    }
  }

  private static String getValue(AttributeValue attrVal) throws WdkModelException, WdkUserException {
    String value;
    if (attrVal == null) {
      return null;
    }
    else {
      value = attrVal.getValue().toString();
    }
    value = value.trim();
    if (value.length() == 0)
      return null;
    return value;
  }
}
