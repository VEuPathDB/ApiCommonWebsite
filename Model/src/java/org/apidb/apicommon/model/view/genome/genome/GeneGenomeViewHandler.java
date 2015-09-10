/**
 * 
 */
package org.niagads.genomics.model.view.genome;

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
        sql.append("    ga.chromosome AS " + COLUMN_SEQUENCE_ID + ", ");
        sql.append("    length(sa.sequence) AS " + COLUMN_SEQUENCE_LENGTH + ", ");
        sql.append("    sa.source_id AS " + COLUMN_CHROMOSOME + ", ");
        sql.append("    ga.start_min AS " + COLUMN_START + ", ");
        sql.append("    ga.end_max AS " + COLUMN_END + ", ");
        sql.append("    ga.name AS " + COLUMN_DESCRIPTION + ", ");
        sql.append("    ga.span AS context, ");
        sql.append("    CASE ga.span WHEN LIKE '%complement%' THEN 0 ELSE 1 END AS "
                + COLUMN_STRAND);
        sql.append(" FROM GeneAttributes ga, ");
        sql.append("      DoTS.ExternalNASequence sa, ");
        sql.append("      (" + idSql + ") idq ");
        sql.append(" WHERE ga.chromosome = sa.source_id ");
        sql.append("  AND ga.source_id = idq.source_id ");

        return sql.toString();
    }

}
