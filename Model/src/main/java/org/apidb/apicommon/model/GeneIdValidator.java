package org.apidb.apicommon.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.gusdb.fgputil.db.SqlUtils;
import org.gusdb.wdk.model.WdkModel;

// TODO: this class appears to be unused.
public class GeneIdValidator {

    private DataSource dataSource;

    public GeneIdValidator(WdkModel wdkModel) {
        try {
          dataSource = wdkModel.getAppDb().getDataSource();
        } catch (Exception e) {
          e.printStackTrace();
        }
    }

    public boolean checkStableIds(String sourceId) {
        ResultSet rs = null;

        StringBuffer sql = new StringBuffer();
        sql.append("SELECT source_id FROM apidbtuning.GeneAttributes ");
        sql.append("WHERE source_id = ? ");
        sql.append("UNION ");
        sql.append("SELECT name FROM apidbtuning.samples ");
        sql.append("WHERE name = ? ");
        sql.append("UNION ");
        sql.append("SELECT source_id FROM DoTS.ExternalNASequence ");
        sql.append("WHERE source_id = ? ");
        sql.append("UNION ");
	sql.append("SELECT id FROM webready.GeneId_p ");
        sql.append("WHERE id = ? ");

        PreparedStatement ps = null;
        try {
            ps = SqlUtils.getPreparedStatement(
                    dataSource, sql.toString(), SqlUtils.Autocommit.OFF);
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
             SqlUtils.closeResultSetAndStatement(rs, ps);
        }

        return false;
    }
}
