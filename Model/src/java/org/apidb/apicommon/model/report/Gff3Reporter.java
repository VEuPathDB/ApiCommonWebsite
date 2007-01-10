/**
 * 
 */
package org.apidb.apicommon.model.report;

import java.util.Iterator;
import java.util.Map;

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

    private final String newline = System.getProperty("line.separator");

    /*
     * (non-Javadoc)
     * 
     * @see org.gusdb.wdk.model.report.IReporter#format(org.gusdb.wdk.model.Answer)
     */
    public String format(Answer answer) throws WdkModelException {
        StringBuffer result = new StringBuffer();

        // output the header
        result.append("##gff-version\t3" + newline);
        result.append("##feature-ontology\tso.obo" + newline);
        result.append("##attribute-ontology\tgff3_attributes.obo" + newline);

        // iterate on each record, and format the result
        String rcName = answer.getQuestion().getRecordClass().getFullName();
        while (answer.hasMoreRecordInstances()) {
            RecordInstance record = answer.getNextRecordInstance();
            if (rcName.equals("GeneRecordClasses.GeneRecordClass")) formatGeneRecord(
                    record, result);
            else formatSequenceRecord(record, result);
        }
        return result.toString();
    }

    private void formatGeneRecord(RecordInstance record, StringBuffer result)
            throws WdkModelException {
        // print the attributes for the record
        result.append(getValue(record.getAttributeValue("gff_seqid")) + "\t");
        result.append(getValue(record.getAttributeValue("gff_source")) + "\t");
        result.append(getValue(record.getAttributeValue("gff_type")) + "\t");
        result.append(getValue(record.getAttributeValue("gff_fstart")) + "\t");
        result.append(getValue(record.getAttributeValue("gff_fend")) + "\t");
        result.append(getValue(record.getAttributeValue("gff_score")) + "\t");
        result.append(getValue(record.getAttributeValue("gff_strand")) + "\t");
        result.append(getValue(record.getAttributeValue("gff_phase")) + "\t");
        result.append("ID=" + getValue(record.getAttributeValue("gff_attr_id")));
        result.append(";Name="
                + getValue(record.getAttributeValue("gff_attr_name")));
        result.append(";description="
                + getValue(record.getAttributeValue("gff_attr_description")));
        result.append(newline);

        // print RNAs
        TableFieldValue rnas = record.getTableValue("gff_GeneRnas");
        Iterator it = rnas.getRows();
        while (it.hasNext()) {
            Map<String, Object> row = (Map<String, Object>) it.next();
            result.append(getValue(row.get("seqid")) + "\t");
            result.append(getValue(row.get("source")) + "\t");
            result.append(getValue(row.get("type")) + "\t");
            result.append(getValue(row.get("fstart")) + "\t");
            result.append(getValue(row.get("fend")) + "\t");
            result.append(getValue(row.get("score")) + "\t");
            result.append(getValue(row.get("strand")) + "\t");
            result.append(getValue(row.get("phase")) + "\t");
            result.append("ID=" + getValue(row.get("attr_id")));
            result.append(";Parent=" + getValue(row.get("attr_parent")));
            result.append(newline);
        }
        rnas.getClose();

        // print CDSs
        TableFieldValue cdss = record.getTableValue("gff_GeneCdss");
        it = cdss.getRows();
        while (it.hasNext()) {
            Map<String, Object> row = (Map<String, Object>) it.next();
            result.append(getValue(row.get("seqid")) + "\t");
            result.append(getValue(row.get("source")) + "\t");
            result.append(getValue(row.get("type")) + "\t");
            result.append(getValue(row.get("fstart")) + "\t");
            result.append(getValue(row.get("fend")) + "\t");
            result.append(getValue(row.get("score")) + "\t");
            result.append(getValue(row.get("strand")) + "\t");
            result.append(getValue(row.get("phase")) + "\t");
            result.append("ID=" + getValue(row.get("attr_id")));
            result.append(";Parent=" + getValue(row.get("attr_parent")));
            result.append(newline);
        }
        cdss.getClose();

        // print EXONs
        TableFieldValue exons = record.getTableValue("gff_GeneExons");
        it = exons.getRows();
        while (it.hasNext()) {
            Map<String, Object> row = (Map<String, Object>) it.next();
            result.append(getValue(row.get("seqid")) + "\t");
            result.append(getValue(row.get("source")) + "\t");
            result.append(getValue(row.get("type")) + "\t");
            result.append(getValue(row.get("fstart")) + "\t");
            result.append(getValue(row.get("fend")) + "\t");
            result.append(getValue(row.get("score")) + "\t");
            result.append(getValue(row.get("strand")) + "\t");
            result.append(getValue(row.get("phase")) + "\t");
            result.append("ID=" + getValue(row.get("attr_id")));
            result.append(";Parent=" + getValue(row.get("attr_parent")));
            result.append(newline);
        }
        exons.getClose();
    }

    private void formatSequenceRecord(RecordInstance record, StringBuffer result) throws WdkModelException {
        // print the attributes for the record
        result.append(getValue(record.getAttributeValue("gff_seqid")) + "\t");
        result.append(getValue(record.getAttributeValue("gff_source")) + "\t");
        result.append(getValue(record.getAttributeValue("gff_type")) + "\t");
        result.append(getValue(record.getAttributeValue("gff_fstart")) + "\t");
        result.append(getValue(record.getAttributeValue("gff_fend")) + "\t");
        result.append(getValue(record.getAttributeValue("gff_score")) + "\t");
        result.append(getValue(record.getAttributeValue("gff_strand")) + "\t");
        result.append(getValue(record.getAttributeValue("gff_phase")) + "\t");
        result.append("ID=" + getValue(record.getAttributeValue("gff_attr_id")));
        result.append(";Name="
                + getValue(record.getAttributeValue("gff_attr_name")));
        result.append(";dbxref="
                + getValue(record.getAttributeValue("gff_attr_dbxref")));
        result.append(";molecule_type="
                + getValue(record.getAttributeValue("gff_attr_molecule_type")));
        result.append(";organism_name="
                + getValue(record.getAttributeValue("gff_attr_organism_name")));
        result.append(";translation_table="
                + getValue(record.getAttributeValue("gff_attr_translation_table")));
        result.append(newline);

    }

    private String getValue(Object object) {
        if (object instanceof AttributeFieldValue) {
            AttributeFieldValue attrVal = (AttributeFieldValue) object;
            return attrVal.getValue().toString();
        } else return object.toString();
    }
}
