package org.apidb.apicommon.model.view;

import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;

public class GeoIsolateViewHandler extends IsolateViewHandler {

    @Override
    public String prepareSql(String idSql) throws WdkModelException,
            WdkUserException {
        StringBuilder sql = new StringBuilder("select country as country, count(country) as total, lat, lng from ( SELECT ");
        sql.append("    i.source_id , ");
        sql.append("    i.latitude AS lat ,");
        sql.append("    i.longitude AS lng ,");
        sql.append("    i.country    ");
        sql.append(" FROM ApidbTuning.PopsetAttributes i, ");
        sql.append("      (" + idSql + ") idq ");
        sql.append(" WHERE  ");
        sql.append("   i.source_id = idq.source_id ");
        sql.append("   AND i.country is not null ");
        sql.append(" ) group by country, lat, lng ");

        return sql.toString();
    }

}
