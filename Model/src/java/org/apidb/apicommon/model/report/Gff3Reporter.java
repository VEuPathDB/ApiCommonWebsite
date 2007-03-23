/**
 * 
 */
package org.apidb.apicommon.model.report;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.gusdb.wdk.model.Answer;
import org.gusdb.wdk.model.AttributeFieldValue;
import org.gusdb.wdk.model.RecordInstance;
import org.gusdb.wdk.model.TableFieldValue;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.report.Reporter;

/**
 * @author xingao
 * 
 */
public class Gff3Reporter extends Reporter {

    private static final String newline = System.getProperty("line.separator");

    private static Set<String> skippedGeneAttributes = new HashSet<String>();
    private static Set<String> skippedSequenceAttributes = new HashSet<String>();

    static {
        String[] skGeneAttrs = new String[] { "primary_key", "product",
                "protein_sequence", "transcript_sequence", "start_min",
                "end_max", "strand", "sequence_id", "cds" };
        for (String skGeneAttr : skGeneAttrs)
            skippedGeneAttributes.add(skGeneAttr);

        String[] skSeqAttrs = new String[] { "organism" };
        for (String skSeqAttr : skSeqAttrs)
            skippedSequenceAttributes.add(skSeqAttr);
    }
    
    public Gff3Reporter(Answer answer) {
        super(answer);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.gusdb.wdk.model.report.IReporter#format(org.gusdb.wdk.model.Answer)
     */
    public void write(OutputStream out) throws WdkModelException {
        StringBuffer header = new StringBuffer();

        // output the header
        header.append("##gff-version\t3" + newline);
        header.append("##feature-ontology\tso.obo" + newline);
        header.append("##attribute-ontology\tgff3_attributes.obo" + newline);

        // iterate on each record, and format the result
        StringBuffer annotation = new StringBuffer();
        StringBuffer fasta = new StringBuffer();
        fasta.append("##FASTA" + newline);

        String rcName = answer.getQuestion().getRecordClass().getFullName();
        while (answer.hasMoreRecordInstances()) {
            RecordInstance record = answer.getNextRecordInstance();
            if (rcName.equals("GeneRecordClasses.GeneRecordClass")) {
                formatGeneRecord(record, annotation, fasta);
            } else {
                formatSequenceRecord(record, header, annotation, fasta);
            }
        }
        
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
        writer.print(header);
        writer.flush();
        writer.print(annotation);
        writer.flush();
        writer.print(fasta);
        writer.flush();
    }

    private void formatGeneRecord(RecordInstance record,
            StringBuffer annotation, StringBuffer fasta)
            throws WdkModelException {
        // print the attributes for the record
        annotation.append(getValue(record.getAttributeValue("gff_seqid"))
                + "\t");
        annotation.append(getValue(record.getAttributeValue("gff_source"))
                + "\t");
        annotation.append(getValue(record.getAttributeValue("gff_type")) + "\t");
        annotation.append(getValue(record.getAttributeValue("gff_fstart"))
                + "\t");
        annotation.append(getValue(record.getAttributeValue("gff_fend")) + "\t");
        annotation.append(getValue(record.getAttributeValue("gff_score"))
                + "\t");
        annotation.append(getValue(record.getAttributeValue("gff_strand"))
                + "\t");
        annotation.append(getValue(record.getAttributeValue("gff_phase"))
                + "\t");
        annotation.append("ID="
                + getValue(record.getAttributeValue("gff_attr_id")));
        annotation.append(";Name="
                + getValue(record.getAttributeValue("gff_attr_name")));
        annotation.append(";description="
                + getValue(record.getAttributeValue("gff_attr_description")));
        annotation.append(newline);

        // get dbxref terms
        TableFieldValue dbxrefs = record.getTableValue("gff_GeneDbxrefs");
        StringBuffer sbDbxrefs = new StringBuffer();
        Iterator it = dbxrefs.getRows();
        while (it.hasNext()) {
            Map<String, Object> row = (Map<String, Object>) it.next();
            String dbxref_value = getValue(row.get("gff_dbxref")).trim();
            if (sbDbxrefs.length() > 0) sbDbxrefs.append(",");
            sbDbxrefs.append(dbxref_value);
        }
        dbxrefs.getClose();

        // get GO terms
        TableFieldValue goTerms = record.getTableValue("GoTerms");
        it = goTerms.getRows();
        StringBuffer sbGoTerms = new StringBuffer();
        while (it.hasNext()) {
            Map<String, Object> row = (Map<String, Object>) it.next();
            String goTerm = getValue(row.get("go_id")).trim();
            if (sbGoTerms.length() > 0) sbGoTerms.append(",");
            sbGoTerms.append(goTerm);
        }
        goTerms.getClose();

        // get EC numbers
        TableFieldValue ecNumbers = record.getTableValue("EcNumber");
        it = ecNumbers.getRows();
        StringBuffer sbEcNumbers = new StringBuffer();
        while (it.hasNext()) {
            Map<String, Object> row = (Map<String, Object>) it.next();
            String ecNumber = getValue(row.get("ec_number")).trim();
            if (sbEcNumbers.length() > 0) sbEcNumbers.append(",");
            sbEcNumbers.append(ecNumber);
        }
        ecNumbers.getClose();

        // print RNAs
        TableFieldValue rnas = record.getTableValue("gff_GeneRnas");
        it = rnas.getRows();
        while (it.hasNext()) {
            Map<String, Object> row = (Map<String, Object>) it.next();
            String rnaId = getValue(row.get("attr_id"));
            annotation.append(getValue(row.get("seqid")) + "\t");
            annotation.append(getValue(row.get("source")) + "\t");
            annotation.append(getValue(row.get("type")) + "\t");
            annotation.append(getValue(row.get("fstart")) + "\t");
            annotation.append(getValue(row.get("fend")) + "\t");
            annotation.append(getValue(row.get("score")) + "\t");
            annotation.append(getValue(row.get("strand")) + "\t");
            annotation.append(getValue(row.get("phase")) + "\t");
            annotation.append("ID=" + rnaId);
            annotation.append(";Name=" + getValue(row.get("attr_name")));
            annotation.append(";Parent=" + getValue(row.get("attr_parent")));
            annotation.append(";description="
                    + getValue(row.get("attr_description")));
            annotation.append(";locus=" + getValue(row.get("attr_locus")));
            // add other attributes
            if (sbDbxrefs.length() > 0)
                annotation.append(";Dbxref=" + sbDbxrefs.toString());
            if (sbGoTerms.length() > 0)
                annotation.append(";Ontology_term=" + sbGoTerms.toString());
            if (sbEcNumbers.length() > 0)
                annotation.append(";ec_number=" + sbEcNumbers.toString());
            annotation.append(newline);

            // output translated protein sequence in fasta
            fasta.append(">" + rnaId + newline);
            String sequence = getValue(row.get("protein_sequence"));
            int offset = 0;
            while (offset < sequence.length()) {
                int endp = offset + Math.min(60, sequence.length() - offset);
                fasta.append(sequence.substring(offset, endp) + newline);
                offset = endp;
            }
        }
        rnas.getClose();

        // print CDSs
        TableFieldValue cdss = record.getTableValue("gff_GeneCdss");
        it = cdss.getRows();
        while (it.hasNext()) {
            Map<String, Object> row = (Map<String, Object>) it.next();
            String cdsId = getValue(row.get("attr_id"));
            annotation.append(getValue(row.get("seqid")) + "\t");
            annotation.append(getValue(row.get("source")) + "\t");
            annotation.append(getValue(row.get("type")) + "\t");
            annotation.append(getValue(row.get("fstart")) + "\t");
            annotation.append(getValue(row.get("fend")) + "\t");
            annotation.append(getValue(row.get("score")) + "\t");
            annotation.append(getValue(row.get("strand")) + "\t");
            annotation.append(getValue(row.get("phase")) + "\t");
            annotation.append("ID=" + cdsId);
            annotation.append(";Parent=" + getValue(row.get("attr_parent")));
            annotation.append(newline);
        }
        cdss.getClose();

        // print EXONs
        TableFieldValue exons = record.getTableValue("gff_GeneExons");
        it = exons.getRows();
        while (it.hasNext()) {
            Map<String, Object> row = (Map<String, Object>) it.next();
            annotation.append(getValue(row.get("seqid")) + "\t");
            annotation.append(getValue(row.get("source")) + "\t");
            annotation.append(getValue(row.get("type")) + "\t");
            annotation.append(getValue(row.get("fstart")) + "\t");
            annotation.append(getValue(row.get("fend")) + "\t");
            annotation.append(getValue(row.get("score")) + "\t");
            annotation.append(getValue(row.get("strand")) + "\t");
            annotation.append(getValue(row.get("phase")) + "\t");
            annotation.append("ID=" + getValue(row.get("attr_id")));
            annotation.append(";Parent=" + getValue(row.get("attr_parent")));
            annotation.append(newline);
        }
        exons.getClose();
    }

    private void formatSequenceRecord(RecordInstance record,
            StringBuffer header, StringBuffer annotation, StringBuffer fasta)
            throws WdkModelException {
        // print the attributes for the record
        String seqId = getValue(record.getAttributeValue("gff_seqid"));
        String fstart = getValue(record.getAttributeValue("gff_fstart"));
        String fend = getValue(record.getAttributeValue("gff_fend"));

        // output header
        header.append("##sequence-region\t" + seqId + "\t" + fstart + "\t"
                + fend + newline);

        // get dbxref terms
        TableFieldValue dbxrefs = record.getTableValue("gff_SequenceDbxrefs");
        StringBuffer sbDbxrefs = new StringBuffer();
        Iterator it = dbxrefs.getRows();
        while (it.hasNext()) {
            Map<String, Object> row = (Map<String, Object>) it.next();
            String dbxref_value = getValue(row.get("gff_dbxref")).trim();
            if (sbDbxrefs.length() > 0) sbDbxrefs.append(",");
            sbDbxrefs.append(dbxref_value);
        }
        dbxrefs.getClose();

        // output annotation
        annotation.append(seqId + "\t");
        annotation.append(getValue(record.getAttributeValue("gff_source"))
                + "\t");
        annotation.append(getValue(record.getAttributeValue("gff_type")) + "\t");
        annotation.append(fstart + "\t");
        annotation.append(fend + "\t");
        annotation.append(getValue(record.getAttributeValue("gff_score"))
                + "\t");
        annotation.append(getValue(record.getAttributeValue("gff_strand"))
                + "\t");
        annotation.append(getValue(record.getAttributeValue("gff_phase"))
                + "\t");
        annotation.append("ID="
                + getValue(record.getAttributeValue("gff_attr_id")));
        annotation.append(";Name="
                + getValue(record.getAttributeValue("gff_attr_name")));
        // add dbxref
        if (sbDbxrefs.length()>0)
            annotation.append(";Dbxref=" + sbDbxrefs.toString());
        annotation.append(";molecule_type="
                + getValue(record.getAttributeValue("gff_attr_molecule_type")));
        annotation.append(";organism_name="
                + getValue(record.getAttributeValue("gff_attr_organism_name")));
        annotation.append(";translation_table="
                + getValue(record.getAttributeValue("gff_attr_translation_table")));
        annotation.append(";topology="
                + getValue(record.getAttributeValue("gff_attr_topology")));
        annotation.append(";localization="
                + getValue(record.getAttributeValue("gff_attr_localization")));
        annotation.append(";size="
                + getValue(record.getAttributeValue("gff_attr_size")));

        annotation.append(newline);

        // output genome sequence in fasta
        String sequence = getValue(record.getAttributeValue("gff_sequence"));
        fasta.append(">" + seqId + newline);
        int offset = 0;
        while (offset < sequence.length()) {
            int endp = offset + Math.min(60, sequence.length() - offset);
            fasta.append(sequence.substring(offset, endp) + newline);
            offset = endp;
        }
    }

    private String getValue(Object object) {
        if (object instanceof AttributeFieldValue) {
            AttributeFieldValue attrVal = (AttributeFieldValue) object;
            return attrVal.getValue().toString();
        } else if (object == null) return null;
        else return object.toString();
    }
}
