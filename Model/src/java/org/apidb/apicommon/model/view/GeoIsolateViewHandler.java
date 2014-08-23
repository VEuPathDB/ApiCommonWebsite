package org.apidb.apicommon.model.view;

import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;

public class GeoIsolateViewHandler extends IsolateViewHandler {

    @Override
    public String prepareSql(String idSql) throws WdkModelException,
            WdkUserException {
        StringBuilder sql = new StringBuilder("select country as country, count(country) as total, data_type, lat, lng from ( SELECT ");
        sql.append("    i.source_id , ");
        sql.append("    i.data_type ,");
        sql.append("    g.lat ,");
        sql.append("    g.lng ,");
        sql.append("    g.country    ");
        sql.append(" FROM ApidbTuning.IsolateAttributes i, ");
        sql.append("       apidb.IsolateGPS g, ");
        sql.append("      (" + idSql + ") idq ");
        sql.append(" WHERE REGEXP_LIKE ( i.ANNOTATED_geographic_location, g.country ) ");
        sql.append("  AND i.source_id = idq.source_id ");
        sql.append(" ) group by country, data_type, lat, lng ");

        return sql.toString();
    }

}
