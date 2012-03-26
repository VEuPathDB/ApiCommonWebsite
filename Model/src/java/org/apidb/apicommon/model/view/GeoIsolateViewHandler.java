package org.apidb.apicommon.model.view;

import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;

public class GeoIsolateViewHandler extends IsolateViewHandler {

    @Override
    public String prepareSql(String idSql) throws WdkModelException,
            WdkUserException {
        StringBuilder sql = new StringBuilder("select country as country, count(country) as total, data_type from ( SELECT ");
        sql.append("    i.source_id , ");
        sql.append("    i.data_type ,");
        sql.append("    v.term  as country    ");
        sql.append(" FROM apidb.isolatevocabulary v, ");
        sql.append("       apidb.isolatemapping m , ");
        sql.append("       ApidbTuning.IsolateAttributes i, ");
        sql.append("      (" + idSql + ") idq ");
        sql.append(" WHERE v.type = 'geographic_location' ");
        sql.append("  AND v.isolate_vocabulary_id = m.isolate_vocabulary_id ");
        sql.append("  AND i.na_sequence_id = m.na_sequence_id ");
        sql.append("  AND i.source_id = idq.source_id ");
        sql.append(" ) group by country, data_type ");

        return sql.toString();
    }

}
