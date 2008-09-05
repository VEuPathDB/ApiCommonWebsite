/**
 * 
 */
package org.apidb.apicommon.model.report;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.gusdb.wdk.model.AnswerValue;
import org.gusdb.wdk.model.AttributeFieldValue;
import org.gusdb.wdk.model.Question;
import org.gusdb.wdk.model.RDBMSPlatformI;
import org.gusdb.wdk.model.RecordInstance;
import org.gusdb.wdk.model.TableFieldValue;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.implementation.SqlUtils;
import org.gusdb.wdk.model.report.Reporter;

/**
 * @author xingao
 * 
 */
public class Gff3Reporter extends Reporter {

    private static Logger logger = Logger.getLogger(Gff3Reporter.class);

    private static final String NEW_LINE = System.getProperty("line.separator");

    public static final String PROPERTY_TABLE_CACHE = "table_cache";
    public static final String PROPERTY_PROJECT_ID_COLUMN = "project_id_column";
    public static final String PROPERTY_RECORD_ID_COLUMN = "record_id_column";

    public static final String PROPERTY_GFF_RECORD_NAME = "gff_record";
    public static final String PROPERTY_GFF_TRANSCRIPT_NAME = "gff_transcript";
    public static final String PROPERTY_GFF_PROTEIN_NAME = "gff_protein";

    public final static String FIELD_HAS_TRANSCRIPT = "hasTranscript";
    public final static String FIELD_HAS_PROTEIN = "hasProtein";

    private String tableCache;
    private String projectIdColumn;
    private String recordIdColumn;
    private String recordName;
    private String proteinName;
    private String transcriptName;

    private boolean hasTranscript = false;
    private boolean hasProtein = false;

    public Gff3Reporter(AnswerValue answer, int startIndex, int endIndex) {
        super(answer, startIndex, endIndex);
    }

    /**
     * (non-Javadoc)
     * 
     * @throws WdkModelException
     * @see org.gusdb.wdk.model.report.Reporter#setProperties(java.util.Map)
     */
    @Override
    public void setProperties(Map<String, String> properties)
            throws WdkModelException {
        super.setProperties(properties);

        // check required properties
        tableCache = properties.get(PROPERTY_TABLE_CACHE);
        recordIdColumn = properties.get(PROPERTY_RECORD_ID_COLUMN);
        projectIdColumn = properties.get(PROPERTY_PROJECT_ID_COLUMN);
        recordName = properties.get(PROPERTY_GFF_RECORD_NAME);
        proteinName = properties.get(PROPERTY_GFF_PROTEIN_NAME);
        transcriptName = properties.get(PROPERTY_GFF_TRANSCRIPT_NAME);

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
            hasTranscript = (value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("true")) ? true
                    : false;
        }

        // include protein
        if (config.containsKey(FIELD_HAS_PROTEIN)) {
            String value = config.get(FIELD_HAS_PROTEIN);
            hasProtein = (value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("true")) ? true
                    : false;
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
     * @see org.gusdb.wdk.model.report.IReporter#format(org.gusdb.wdk.model.AnswerValue)
     */
    public void write(OutputStream out) throws WdkModelException {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));

        // write header
        writeHeader(writer);

        // write records
        writeRecords(writer);

        // write sequences
        writer.println("##FASTA");
        writeSequences(writer);
    }

    void writeHeader(PrintWriter writer) throws WdkModelException {
        writer.println("##gff-version\t3");
        writer.println("##feature-ontology\tso.obo");
        writer.println("##attribute-ontology\tgff3_attributes.obo");
        writer.flush();

        // get the sequence regions
        Map<String, int[]> regions = new LinkedHashMap<String, int[]>();

        // get page based answers with a maximum size (defined in
        // PageAnswerValueIterator)
        for (AnswerValue answer : this) {
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

    void writeRecords(PrintWriter writer) throws WdkModelException {
        Question question = getQuestion();
        String rcName = question.getRecordClass().getFullName();
        WdkModel wdkModel = question.getWdkModel();
        RDBMSPlatformI platform = wdkModel.getPlatform();

        // check if we need to use project id
        boolean hasProjectId = hasProjectId();

        // check if we need to insert into cache
        PreparedStatement psCache = null;
        PreparedStatement psCheck = null;
        try {
            if (tableCache != null) {
                // want to cache the table content
                DataSource dataSource = platform.getDataSource();
                psCache = SqlUtils.getPreparedStatement(dataSource, "INSERT "
                        + "INTO " + tableCache + " (" + recordIdColumn
                        + ", table_name, row_count, content"
                        + (hasProjectId ? ", " + projectIdColumn + ")" : ")")
                        + " VALUES (?, ?, ?, ?" + (hasProjectId ? ", ?)" : ")"));
                psCheck = SqlUtils.getPreparedStatement(dataSource, "SELECT "
                        + "count(*) AS cache_count FROM "
                        + tableCache
                        + " WHERE "
                        + recordIdColumn
                        + " = ? "
                        + " AND table_name IN ('"
                        + recordName
                        + "')"
                        + (hasProjectId ? " AND " + projectIdColumn + " = ?"
                                : ""));
            }

            // get page based answers with a maximum size (defined in
            // PageAnswerValueIterator)
            for (AnswerValue answer : this) {
                while (answer.hasMoreRecordInstances()) {
                    RecordInstance record = answer.getNextRecordInstance();

                    StringBuffer recordBuffer = new StringBuffer();

                    // read and format record content
                    if (rcName.equals("SequenceRecordClasses.SequenceRecordClass")) {
                        formatSequenceRecord(record, recordBuffer);
                    } else if (rcName.equals("GeneRecordClasses.GeneRecordClass")) {
                        formatGeneRecord(record, recordBuffer);
                    } else {
                        throw new WdkModelException("Unsupported record type: "
                                + rcName);
                    }
                    String content = recordBuffer.toString();

                    // check if the record has been cached
                    boolean hasCached = false;

                    if (tableCache != null) {
                        psCheck.setString(1,
                                record.getPrimaryKey().getRecordId());
                        if (hasProjectId) {
                            String projectId = record.getPrimaryKey().getProjectId();
                            psCheck.setString(2, projectId);
                        }
                        ResultSet rs = psCheck.executeQuery();
                        try {
                            rs.next();
                            int count = rs.getInt("cache_count");
                            if (count > 0) hasCached = true;
                        } finally {
                            rs.close();
                        }
                    }

                    // check if needs to insert into cache table
                    if (tableCache != null && !hasCached) {
                        // save into table cache
                        String recordId = record.getPrimaryKey().getRecordId();
                        psCache.setString(1, recordId);
                        psCache.setString(2, recordName);
                        psCache.setInt(3, 1);
                        platform.updateClobData(psCache, 4, content, false);
                        if (hasProjectId) {
                            String projectId = record.getPrimaryKey().getProjectId();
                            psCache.setString(5, projectId);
                        }
                        psCache.executeUpdate();
                    }

                    // output the result
                    writer.print(content);
                    writer.flush();
                }
            }
        } catch (SQLException ex) {
            throw new WdkModelException(ex);
        } finally {
            try {
                SqlUtils.closeStatement(psCheck);
                SqlUtils.closeStatement(psCache);
            } catch (SQLException ex) {
                throw new WdkModelException(ex);
            }
        }
    }

    private void formatGeneRecord(RecordInstance record,
            StringBuffer recordBuffer) throws WdkModelException {
        // get common fields from the record
        readCommonFields(record, recordBuffer);

        // get the rest of the attributes
        String webId = readField(record, "gff_attr_web_id");
        if (webId != null) recordBuffer.append(";web_id=" + webId);
        String locusTag = readField(record, "gff_attr_locus_tag");
        if (locusTag != null) recordBuffer.append(";locus_tag=" + locusTag);
        String size = readField(record, "gff_attr_size");
        if (size != null) recordBuffer.append(";size=" + size);

        // get aliases
        TableFieldValue alias = record.getTableValue("GeneGffAliases");
        StringBuffer sbAlias = new StringBuffer();
        Iterator<Map<String, Object>> it = alias.getRows();
        while (it.hasNext()) {
            Map<String, Object> row = it.next();
            String alias_value = getValue(row.get("gff_alias")).trim();
            if (sbAlias.length() > 0) sbAlias.append(",");
            sbAlias.append(alias_value);
        }
        alias.getClose();
        if (sbAlias.length() > 0)
            recordBuffer.append(";Alias=" + sbAlias.toString());

        recordBuffer.append(NEW_LINE);

        // get GO terms
        TableFieldValue goTerms = record.getTableValue("GeneGffGoTerms");
        it = goTerms.getRows();
        Set<String> termSet = new LinkedHashSet<String>();
        while (it.hasNext()) {
            Map<String, Object> row = it.next();
            String goTerm = getValue(row.get("gff_go_id")).trim();
            termSet.add(goTerm);
        }
        goTerms.getClose();
        StringBuffer sbGoTerms = new StringBuffer();
        for (String termName : termSet) {
            if (sbGoTerms.length() > 0) sbGoTerms.append(",");
            sbGoTerms.append(termName);
        }

        // get dbxref terms
        TableFieldValue dbxrefs = record.getTableValue("GeneGffDbxrefs");
        StringBuffer sbDbxrefs = new StringBuffer();
        it = dbxrefs.getRows();
        while (it.hasNext()) {
            Map<String, Object> row = it.next();
            String dbxref_value = getValue(row.get("gff_dbxref")).trim();
            if (sbDbxrefs.length() > 0) sbDbxrefs.append(",");
            sbDbxrefs.append(dbxref_value);
        }
        dbxrefs.getClose();

        // print RNAs
        TableFieldValue rnas = record.getTableValue("GeneGffRnas");
        it = rnas.getRows();
        while (it.hasNext()) {
            Map<String, Object> row = it.next();

            // read common fields
            readCommonFields(row, recordBuffer);

            // read other fields
            recordBuffer.append(";Parent=" + readField(row, "gff_attr_parent"));

            // add GO terms in mRNA
            if (sbGoTerms.length() > 0)
                recordBuffer.append(";Ontology_term=" + sbGoTerms.toString());

            // add dbxref in mRNA
            if (sbDbxrefs.length() > 0)
                recordBuffer.append(";Dbxref=" + sbDbxrefs.toString());

            recordBuffer.append(NEW_LINE);
        }
        rnas.getClose();

        // print CDSs
        TableFieldValue cdss = record.getTableValue("GeneGffCdss");
        it = cdss.getRows();
        while (it.hasNext()) {
            Map<String, Object> row = it.next();

            // read common fields
            readCommonFields(row, recordBuffer);

            // read other fields
            recordBuffer.append(";Parent=" + readField(row, "gff_attr_parent"));

            recordBuffer.append(NEW_LINE);
        }
        cdss.getClose();

        // print EXONs
        TableFieldValue exons = record.getTableValue("GeneGffExons");
        it = exons.getRows();
        while (it.hasNext()) {
            Map<String, Object> row = it.next();

            // read common fields
            readCommonFields(row, recordBuffer);

            // read other fields
            recordBuffer.append(";Parent=" + readField(row, "gff_attr_parent"));

            recordBuffer.append(NEW_LINE);
        }
        exons.getClose();
    }

    private void formatSequenceRecord(RecordInstance record,
            StringBuffer recordBuffer) throws WdkModelException {
        // get common fields from the record
        readCommonFields(record, recordBuffer);

        // read other fields
        String webId = readField(record, "gff_attr_web_id");
        if (webId != null) recordBuffer.append(";web_id=" + webId);
        recordBuffer.append(";molecule_type="
                + readField(record, "gff_attr_molecule_type"));
        recordBuffer.append(";organism_name="
                + readField(record, "gff_attr_organism_name"));
        recordBuffer.append(";translation_table="
                + readField(record, "gff_attr_translation_table"));
        recordBuffer.append(";topology="
                + readField(record, "gff_attr_topology"));
        recordBuffer.append(";localization="
                + readField(record, "gff_attr_localization"));

        // get dbxref terms
        TableFieldValue dbxrefs = record.getTableValue("SequenceGffDbxrefs");
        StringBuffer sbDbxrefs = new StringBuffer();
        Iterator<Map<String, Object>> it = dbxrefs.getRows();
        while (it.hasNext()) {
            Map<String, Object> row = it.next();
            String dbxref_value = getValue(row.get("gff_dbxref")).trim();
            if (sbDbxrefs.length() > 0) sbDbxrefs.append(",");
            sbDbxrefs.append(dbxref_value);
        }
        dbxrefs.getClose();
        if (sbDbxrefs.length() > 0)
            recordBuffer.append(";Dbxref=" + sbDbxrefs.toString());

        recordBuffer.append(NEW_LINE);
    }

    void writeSequences(PrintWriter writer) throws WdkModelException {
        Question question = getQuestion();
        String rcName = question.getRecordClass().getFullName();
        WdkModel wdkModel = question.getWdkModel();
        RDBMSPlatformI platform = wdkModel.getPlatform();

        // check if we need to use project id
        boolean hasProjectId = hasProjectId();

        // check if we need to insert into cache
        PreparedStatement psCache = null;
        PreparedStatement psCheck = null;
        try {
            if (tableCache != null) {
                // want to cache the table content
                DataSource dataSource = platform.getDataSource();
                psCache = SqlUtils.getPreparedStatement(dataSource, "INSERT "
                        + "INTO " + tableCache + " (" + recordIdColumn
                        + ", table_name, row_count, content"
                        + (hasProjectId ? ", " + projectIdColumn + ")" : ")")
                        + " VALUES (?, ?, ?, ?" + (hasProjectId ? ", ?)" : ")"));
                psCheck = SqlUtils.getPreparedStatement(dataSource, "SELECT "
                        + "count(*) AS cache_count FROM "
                        + tableCache
                        + " WHERE "
                        + recordIdColumn
                        + " = ? "
                        + " AND table_name IN ('"
                        + transcriptName
                        + "', '"
                        + proteinName
                        + "')"
                        + (hasProjectId ? " AND " + projectIdColumn + " = ?"
                                : ""));
            }

            // get page based answers with a maximum size (defined in
            // PageAnswerValueIterator)
            for (AnswerValue answer : this) {
                while (answer.hasMoreRecordInstances()) {
                    RecordInstance record = answer.getNextRecordInstance();
                    String recordId = record.getPrimaryKey().getRecordId();

                    // read and format record content
                    if (rcName.equals("SequenceRecordClasses.SequenceRecordClass")) {
                        // get genome sequence
                        String sequence = getValue(record.getAttributeValue("gff_sequence"));
                        if (sequence != null && sequence.length() > 0) {
                            // output the sequence
                            sequence = formatSequence(recordId, sequence);
                            writer.print(sequence);
                            writer.flush();
                        }
                    } else if (rcName.equals("GeneRecordClasses.GeneRecordClass")) {
                        boolean hasCached = false;
                        // get transcript, if needed
                        if (hasTranscript) {
                            String sequence = getValue(record.getAttributeValue("gff_transcript_sequence"));
                            if (sequence != null && sequence.length() > 0) {
                                sequence = formatSequence(recordId, sequence);

                                // check if the record has been cached
                                if (tableCache != null) {
                                    psCheck.setString(1, recordId);
                                    if (hasProjectId) {
                                        String projectId = record.getPrimaryKey().getProjectId();
                                        psCheck.setString(2, projectId);
                                    }
                                    ResultSet rs = psCheck.executeQuery();
                                    try {
                                        rs.next();
                                        int count = rs.getInt("cache_count");
                                        if (count > 0) hasCached = true;
                                    } finally {
                                        rs.close();
                                    }
                                }

                                // check if needs to insert into cache table
                                if (tableCache != null && !hasCached) {
                                    // save into table cache
                                    psCache.setString(1, recordId);
                                    psCache.setString(2, transcriptName);
                                    psCache.setInt(3, 1);
                                    platform.updateClobData(psCache, 4,
                                            sequence, false);
                                    if (hasProjectId) {
                                        String projectId = record.getPrimaryKey().getProjectId();
                                        psCache.setString(5, projectId);
                                    }
                                    psCache.executeUpdate();
                                }

                                // output the sequence
                                writer.print(sequence);
                                writer.flush();
                            }
                        }

                        // get protein sequence, if needed
                        if (hasProtein) {
                            // get the first CDS id
                            String cdsId = null;
                            TableFieldValue cdss = record.getTableValue("GeneGffCdss");
                            Iterator<Map<String, Object>> it = cdss.getRows();
                            if (it.hasNext()) {
                                Map<String, Object> row = it.next();
                                cdsId = readField(row, "gff_attr_id");
                            }
                            cdss.getClose();

                            // print CDSs
                            TableFieldValue rnas = record.getTableValue("GeneGffRnas");
                            it = rnas.getRows();
                            while (it.hasNext()) {
                                Map<String, Object> row = it.next();

                                String sequence = getValue(row.get("gff_protein_sequence"));
                                if (cdsId != null && sequence != null
                                        && sequence.length() > 0) {
                                    sequence = formatSequence(cdsId, sequence);

                                    // check if needs to insert into cache table
                                    if (tableCache != null && !hasCached) {
                                        // save into table cache

                                        psCache.setString(1, recordId);
                                        psCache.setString(2, proteinName);
                                        psCache.setInt(3, 1);
                                        platform.updateClobData(psCache, 4,
                                                sequence, false);
                                        if (hasProjectId) {
                                            String projectId = record.getPrimaryKey().getProjectId();
                                            psCache.setString(5, projectId);
                                        }
                                        try {
                                            psCache.executeUpdate();
                                        } finally {
                                            String projectId = record.getPrimaryKey().getProjectId();
                                            System.out.println(recordId + ":"
                                                    + projectId + ":"
                                                    + proteinName);

                                        }

                                    }

                                    // output the sequence
                                    writer.print(sequence);
                                    writer.flush();
                                }
                            }
                            rnas.getClose();
                        }
                    } else {
                        throw new WdkModelException("Unsupported record type: "
                                + rcName);
                    }
                }
            }
        } catch (SQLException ex) {
            throw new WdkModelException(ex);
        } finally {
            try {
                if (psCache != null) psCache.close();
                if (psCheck != null) psCheck.close();
            } catch (SQLException ex) {
                throw new WdkModelException(ex);
            }
        }
    }

    private void readCommonFields(Object object, StringBuffer buffer)
            throws WdkModelException {
        buffer.append(readField(object, "gff_seqid") + "\t");
        buffer.append(readField(object, "gff_source") + "\t");
        buffer.append(readField(object, "gff_type") + "\t");
        buffer.append(readField(object, "gff_fstart") + "\t");
        buffer.append(readField(object, "gff_fend") + "\t");
        buffer.append(readField(object, "gff_score") + "\t");
        buffer.append(readField(object, "gff_strand") + "\t");
        buffer.append(readField(object, "gff_phase") + "\t");
        String id = readField(object, "gff_attr_id");
        buffer.append("ID=" + id);

        String name = readField(object, "gff_attr_name");
        if (name == null) name = id;
        try {
            buffer.append(";Name=" + URLEncoder.encode(name, "utf-8"));
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }

        String description = readField(object, "gff_attr_description");
        if (description == null) description = name;
        try {
            buffer.append(";description="
                    + URLEncoder.encode(description, "utf-8"));
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }

        buffer.append(";size=" + readField(object, "gff_attr_size"));
    }

    private String readField(Object object, String field)
            throws WdkModelException {
        Object value;
        if (object instanceof RecordInstance) {
            RecordInstance record = (RecordInstance) object;
            value = record.getAttributeValue(field);
        } else {
            Map<String, Object> row = (Map<String, Object>) object;
            value = row.get(field);
        }
        return getValue(value);
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

    private String formatSequence(String id, String sequence) {
        if (sequence == null) return null;

        StringBuffer buffer = new StringBuffer();
        buffer.append(">" + id + NEW_LINE);
        int offset = 0;
        while (offset < sequence.length()) {
            int endp = offset + Math.min(60, sequence.length() - offset);
            buffer.append(sequence.substring(offset, endp) + NEW_LINE);
            offset = endp;
        }
        return buffer.toString();
    }
}
