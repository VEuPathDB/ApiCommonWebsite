/**
 * 
 */
package org.apidb.apicommon.model.report;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.gusdb.wdk.model.Answer;
import org.gusdb.wdk.model.AttributeFieldValue;
import org.gusdb.wdk.model.RDBMSPlatformI;
import org.gusdb.wdk.model.RecordInstance;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.implementation.SqlUtils;
import org.gusdb.wdk.model.report.Reporter;

/**
 * @author xingao
 * 
 */
public class Gff3CachedReporter extends Reporter {

    private static Logger logger = Logger.getLogger(Gff3Reporter.class);

    public static final String PROPERTY_TABLE_CACHE = "table_cache";
    public static final String PROPERTY_RECORD_ID_COLUMN = "record_id_column";

    public static final String PROPERTY_GFF_RECORD_NAME = "gff_record";
    public static final String PROPERTY_GFF_TRANSCRIPT_NAME = "gff_transcript";
    public static final String PROPERTY_GFF_PROTEIN_NAME = "gff_protein";

    public final static String FIELD_HAS_TRANSCRIPT = "hasTranscript";
    public final static String FIELD_HAS_PROTEIN = "hasProtein";

    private String tableCache;
    private String recordIdColumn;
    private String recordName;
    private String proteinName;
    private String transcriptName;

    private boolean hasTranscript = false;
    private boolean hasProtein = false;

    public Gff3CachedReporter(Answer answer, int startIndex, int endIndex) {
        super(answer, startIndex, endIndex);
    }

    /**
     * (non-Javadoc)
     * 
     * @see org.gusdb.wdk.model.report.Reporter#setProperties(java.util.Map)
     */
    @Override
    public void setProperties(Map<String, String> properties)
            throws WdkModelException {
        super.setProperties(properties);

        // check required properties
        tableCache = properties.get(PROPERTY_TABLE_CACHE);
        recordIdColumn = properties.get(PROPERTY_RECORD_ID_COLUMN);
        recordName = properties.get(PROPERTY_GFF_RECORD_NAME);
        proteinName = properties.get(PROPERTY_GFF_PROTEIN_NAME);
        transcriptName = properties.get(PROPERTY_GFF_TRANSCRIPT_NAME);

        if (tableCache == null || tableCache.length() == 0)
            throw new WdkModelException("The required property for reporter "
                    + this.getClass().getName() + ", " + PROPERTY_TABLE_CACHE
                    + ", is missing");
        if (recordIdColumn == null || recordIdColumn.length() == 0)
            throw new WdkModelException("The required property for reporter "
                    + this.getClass().getName() + ", "
                    + PROPERTY_RECORD_ID_COLUMN + ", is missing");
        if (recordName == null || recordName.length() == 0)
            throw new WdkModelException("The required property for reporter "
                    + this.getClass().getName() + ", "
                    + PROPERTY_GFF_RECORD_NAME + ", is missing");
        if (proteinName == null || proteinName.length() == 0)
            throw new WdkModelException("The required property for reporter "
                    + this.getClass().getName() + ", "
                    + PROPERTY_GFF_PROTEIN_NAME + ", is missing");
        if (transcriptName == null || transcriptName.length() == 0)
            throw new WdkModelException("The required property for reporter "
                    + this.getClass().getName() + ", "
                    + PROPERTY_GFF_PROTEIN_NAME + ", is missing");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.gusdb.wdk.model.report.Reporter#configure(java.util.Map)
     */
    @Override
    public void configure(Map<String, String> config) {
        super.configure(config);

        // include transcript
        if (config.containsKey(FIELD_HAS_TRANSCRIPT)) {
            String value = config.get(FIELD_HAS_TRANSCRIPT);
            hasTranscript = (value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("true"))
                    ? true : false;
        }

        // include protein
        if (config.containsKey(FIELD_HAS_PROTEIN)) {
            String value = config.get(FIELD_HAS_PROTEIN);
            hasProtein = (value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("true"))
                    ? true : false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.gusdb.wdk.model.report.Reporter#getHttpContentType()
     */
    @Override
    public String getHttpContentType() {
        if (format.equalsIgnoreCase("text")) {
            return "text/plain";
        } else { // use the default content type defined in the parent class
            return super.getHttpContentType();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.gusdb.wdk.model.report.Reporter#getDownloadFileName()
     */
    @Override
    public String getDownloadFileName() {
        logger.info("Internal format: " + format);
        String name = getQuestion().getName();
        if (format.equalsIgnoreCase("text")) {
            return name + ".gff";
        } else { // use the default file name defined in the parent
            return super.getDownloadFileName();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.gusdb.wdk.model.report.IReporter#format(org.gusdb.wdk.model.Answer)
     */
    public void write(OutputStream out) throws WdkModelException {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));

        // this reporter only works for GeneRecordClasses.GeneRecordClass
        String rcName = getQuestion().getRecordClass().getFullName();
        if (!rcName.equals("GeneRecordClasses.GeneRecordClass"))
            throw new WdkModelException("Unsupported record type: " + rcName);

        // write header
        writeHeader(writer);

        // write record
        writeRecords(writer);

        // write sequence
        if (hasTranscript || hasProtein) writeSequences(writer);
    }

    private void writeHeader(PrintWriter writer) throws WdkModelException {
        writer.println("##gff-version\t3");
        writer.println("##feature-ontology\tso.obo");
        writer.println("##attribute-ontology\tgff3_attributes.obo");
        writer.flush();

        // get the sequence regions
        Map<String, int[]> regions = new LinkedHashMap<String, int[]>();

        // get page based answers with a maximum size (defined in
        // PageAnswerIterator)
        for (Answer answer : this) {
            while (answer.hasMoreRecordInstances()) {
                RecordInstance record = answer.getNextRecordInstance();
                String seqId = getValue(record.getAttributeValue("gff_seqid"));
                int start = Integer.parseInt(getValue(record.getAttributeValue("gff_fstart")));
                int stop = Integer.parseInt(getValue(record.getAttributeValue("gff_fend")));
                if (regions.containsKey(seqId)) {
                    int[] region = regions.get(seqId);
                    if (region[0] > start) region[0] = start;
                    if (region[1] < stop) region[1] = stop;
                    regions.put(seqId, region);
                } else {
                    int[] region = { start, stop };
                    regions.put(seqId, region);
                }
            }
        }

        // put sequence id into the header
        for (String seqId : regions.keySet()) {
            int[] region = regions.get(seqId);
            writer.println("##sequence-region\t" + seqId + "\t" + region[0]
                    + "\t" + region[1]);
        }
        writer.flush();
    }

    private void writeRecords(PrintWriter writer) throws WdkModelException {
        // construct the SQL to retrieve table cache
        String answerCache = getCacheTableName();
        String indexColumn = getResultIndexColumn();
        String sortingColumn = getSortingIndexColumn();
        int sortingIndex = getSortingIndex();
        boolean hasProjectId = hasProjectId();

        StringBuffer sql = new StringBuffer("SELECT tc.content");
        sql.append(" FROM " + answerCache + " ac, " + tableCache + " tc");
        sql.append(" WHERE tc." + recordIdColumn + " = ac." + recordIdColumn);
        sql.append(" AND tc.table_name = '" + recordName + "'");
        sql.append(" AND ac." + sortingColumn + " = " + sortingIndex);
        if (hasProjectId) sql.append(" AND tc.project_id = ac.project_id");
        sql.append(" ORDER BY ac." + indexColumn);

        RDBMSPlatformI platform = getQuestion().getWdkModel().getPlatform();

        // get the result from database
        ResultSet rsTable = null;
        try {
            rsTable = SqlUtils.getResultSet(platform.getDataSource(),
                    sql.toString());

            while (rsTable.next()) {
                String content = platform.getClobData(rsTable, "content");
                writer.print(content);
                writer.flush();
            }
        } catch (SQLException ex) {
            throw new WdkModelException(ex);
        } finally {
            try {
                SqlUtils.closeResultSet(rsTable);
            } catch (SQLException ex) {
                throw new WdkModelException(ex);
            }
        }
    }

    private void writeSequences(PrintWriter writer) throws WdkModelException {
        // construct in clause
        StringBuffer sqlIn = new StringBuffer();
        if (hasTranscript) sqlIn.append("'" + transcriptName + "'");
        if (hasProtein) {
            if (sqlIn.length() > 0) sqlIn.append(", ");
            sqlIn.append("'" + proteinName + "'");
        }

        // construct the SQL to retrieve table cache
        String answerCache = getCacheTableName();
        String indexColumn = getResultIndexColumn();
        String sortingColumn = getSortingIndexColumn();
        int sortingIndex = getSortingIndex();
        boolean hasProjectId = hasProjectId();

        StringBuffer sql = new StringBuffer("SELECT tc.content");
        sql.append(" FROM " + answerCache + " ac, " + tableCache + " tc");
        sql.append(" WHERE tc." + recordIdColumn + " = ac." + recordIdColumn);
        sql.append(" AND tc.table_name IN (" + sqlIn.toString() + ")");
        sql.append(" AND ac." + sortingColumn + " = " + sortingIndex);
        if (hasProjectId) sql.append(" AND tc.project_id = ac.project_id");
        sql.append(" ORDER BY ac." + indexColumn + " ASC, tc.table_name ASC");

        RDBMSPlatformI platform = getQuestion().getWdkModel().getPlatform();

        writer.println("##FASTA");

        // get the result from database
        ResultSet rsTable = null;
        try {
            rsTable = SqlUtils.getResultSet(platform.getDataSource(),
                    sql.toString());

            while (rsTable.next()) {
                String content = platform.getClobData(rsTable, "content");
                writer.print(content);
                writer.flush();
            }
        } catch (SQLException ex) {
            throw new WdkModelException(ex);
        } finally {
            try {
                SqlUtils.closeResultSet(rsTable);
            } catch (SQLException ex) {
                throw new WdkModelException(ex);
            }
        }
    }

    private String getValue(Object object) {
        String value;
        if (object == null) {
            return null;
        } else if (object instanceof AttributeFieldValue) {
            AttributeFieldValue attrVal = (AttributeFieldValue) object;
            value = attrVal.getValue().toString();
        } else {
            value = object.toString();
        }
        value = value.trim();
        if (value.length() == 0) return null;
        return value;
    }
}