package org.apidb.apicommon.controller;

import java.io.PrintWriter;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.gusdb.wdk.controller.action.ActionUtility;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.dbms.SqlUtils;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;
import org.json.JSONArray;
import org.json.JSONObject;

public class GetGenomeInfoAction extends Action {

    private static Logger logger = Logger.getLogger(GetGenomeInfoAction.class);

    private static class Sequence {
        public String sourceId;
        public String name;
        public int order;
        public long length;
    }

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        logger.debug("entering GetGenomeInfoAction...");

        WdkModelBean wdkModelBean = ActionUtility.getWdkModel(servlet);
        WdkModel wdkModel = wdkModelBean.getModel();
        String sql = "SELECT organism, source_id, length, "
                + "          chromosome, chromosome_order_num"
                + " FROM apidb.SequenceAttributes "
                + " WHERE chromosome_order_num IS NOT NULL "
                + " ORDER BY organism, chromosome_order_num";
        DataSource dataSource = wdkModel.getQueryPlatform().getDataSource();

        ResultSet resultSet = null;
        try {
            Map<String, Map<String, Sequence>> organisms = new LinkedHashMap<String, Map<String, Sequence>>();
            resultSet = SqlUtils.executeQuery(wdkModel, dataSource, sql,
                    "apidb-get-genome-info");
            while (resultSet.next()) {
                String organism = resultSet.getString("organim");
                Map<String, Sequence> sequences = organisms.get(organism);
                if (sequences == null) {
                    sequences = new LinkedHashMap<String, Sequence>();
                    organisms.put(organism, sequences);
                }
                
                Sequence sequence = new Sequence();
                sequence.sourceId = resultSet.getString("source_id");
                sequence.name = resultSet.getString("name");
                sequence.order = resultSet.getInt("order");
                sequence.length = resultSet.getLong("length");
                sequences.put(sequence.sourceId, sequence);
            }

            // format the organisms into JSON array.
            JSONArray jsOrganisms = new JSONArray();
            for(String organism : organisms.keySet()) {
                Map<String, Sequence> sequences = organisms.get(organism);
                JSONArray jsSequences = new JSONArray();
                for(Sequence sequence : sequences.values()) {
                    JSONObject jsSequence = new JSONObject();
                    jsSequence.put("sourceId", sequence.sourceId);
                    jsSequence.put("chromosome", sequence.name);
                    jsSequence.put("chromosome_order_num", sequence.order);
                    jsSequence.put("length", sequence.length);
                    
                    jsSequences.put(jsSequence);
                }
                JSONObject jsOrganism = new JSONObject();
                jsOrganism.put("organism", organism);
                jsOrganism.put("sequences", jsSequences);
                
                jsOrganisms.put(jsOrganism);
            }
            
            
            PrintWriter writer = response.getWriter();
            writer.print(jsOrganisms.toString());
            return null;

        } finally {
            SqlUtils.closeResultSet(resultSet);
        }
    }
}
