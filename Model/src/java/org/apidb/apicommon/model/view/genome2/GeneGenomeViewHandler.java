/**
 * 
 */
package org.apidb.apicommon.model.view.genome2;

import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;

/**
 * @author jerric
 * 
 */
public class GeneGenomeViewHandler extends GenomeViewHandler {

    @Override
    public String prepareSql(String idSql) throws WdkModelException,
            WdkUserException {
        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append("    ga.source_id AS " + COLUMN_SOURCE_ID + ", ");
        sql.append("    ga.sequence_id AS " + COLUMN_SEQUENCE_ID + ", ");
        sql.append("    sa.length AS " + COLUMN_SEQUENCE_LENGTH + ", ");
        sql.append("    sa.chromosome AS " + COLUMN_CHROMOSOME + ", ");
        sql.append("    sa.organism AS " + COLUMN_ORGANISM + ", ");
        sql.append("    ga.start_min AS " + COLUMN_START + ", ");
        sql.append("    ga.end_max AS " + COLUMN_END + ", ");
        sql.append("    ga.product AS " + COLUMN_DESCRIPTION + ", ");
        sql.append("    ga.context_start || '..' || ga.context_end AS context, ");
        sql.append("    CASE ga.strand WHEN 'forward' THEN 1 ELSE 0 END AS "
                + COLUMN_STRAND);
        sql.append(" FROM ApidbTuning.GeneAttributes ga, ");
        sql.append("      ApiDBTuning.SequenceAttributes sa, ");
        sql.append("      (" + idSql + ") idq ");
        sql.append(" WHERE ga.sequence_id = sa.source_id ");
        sql.append("  AND ga.source_id = idq.source_id ");

        return sql.toString();
    }

}
