package org.apidb.apicommon.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.gusdb.wdk.model.dbms.DBPlatform;
import org.gusdb.wdk.model.dbms.SqlUtils;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;

public class GeneIdValidator {

    private DBPlatform platform;

    public GeneIdValidator(WdkModelBean wdkModelBean) {
        try {
            // WdkModel wdkModel = WdkModel.construct(projectId, gusHome);
            platform = wdkModelBean.getModel().getQueryPlatform();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean checkStableIds(String sourceId) {
        ResultSet rs = null;

        StringBuffer sql = new StringBuffer();
        sql.append("SELECT source_id FROM ApiDB.GeneAttributes ");
        sql.append("WHERE source_id = ? ");
        sql.append("UNION ");
        sql.append("SELECT source_id FROM ApiDB.IsolateAttributes ");
        sql.append("WHERE source_id = ? ");
        sql.append("UNION ");
        sql.append("SELECT source_id FROM DoTS.ExternalNASequence ");
        sql.append("WHERE source_id = ? ");

        try {
            PreparedStatement ps = SqlUtils.getPreparedStatement(
                    platform.getDataSource(), sql.toString());
            ps.setString(1, sourceId);
            ps.setString(2, sourceId);
            ps.setString(3, sourceId);
            rs = ps.executeQuery();

            while (rs.next()) {
                rs.getString("source_id");
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }
}
