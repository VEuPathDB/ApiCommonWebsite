package org.apidb.apicommon.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.gusdb.fgputil.db.SqlUtils;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;

public class GeneIdValidator {

    private DataSource dataSource;

    public GeneIdValidator(WdkModelBean wdkModelBean) {
        try {
            // WdkModel wdkModel = WdkModel.construct(projectId, gusHome);
            dataSource = wdkModelBean.getModel().getAppDb().getDataSource();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean checkStableIds(String sourceId) {
        ResultSet rs = null;

        StringBuffer sql = new StringBuffer();
        sql.append("SELECT source_id FROM ApidbTuning.GeneAttributes ");
        sql.append("WHERE source_id = ? ");
        sql.append("UNION ");
        sql.append("SELECT source_id FROM ApidbTuning.IsolateAttributes ");
        sql.append("WHERE source_id = ? ");
        sql.append("UNION ");
        sql.append("SELECT source_id FROM DoTS.ExternalNASequence ");
        sql.append("WHERE source_id = ? ");
				sql.append("UNION ");
				sql.append("SELECT id FROM ApidbTuning.Geneid ");
        sql.append("WHERE id = ? ");

        try {
            PreparedStatement ps = SqlUtils.getPreparedStatement(
                    dataSource, sql.toString());
            ps.setString(1, sourceId);
            ps.setString(2, sourceId);
            ps.setString(3, sourceId);
						ps.setString(4, sourceId);
            rs = ps.executeQuery();

            while (rs.next()) {
                rs.getString("source_id");
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
             // close the connection
             SqlUtils.closeResultSetAndStatement(rs);
        }

        return false;
    }
}
