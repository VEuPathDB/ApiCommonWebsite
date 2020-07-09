package org.apidb.apicommon.service.services.dataPlotter;

import static org.gusdb.fgputil.db.stream.ResultSetInputStream.getResultSetStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.db.stream.ResultSetToJsonConverter;
import org.gusdb.fgputil.functional.Functions;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.service.service.AbstractWdkService;
import org.json.JSONArray;
import org.json.JSONObject;


@Path("/profileSet")
public class ProfileSetService extends AbstractWdkService {

  @SuppressWarnings("unused")
  private static final Logger LOG = Logger.getLogger(ProfileSetService.class);

  private static final int FETCH_SIZE = 10000;

  private Response getStreamingResponse(String sql, String queryName, String errorMsgOnFail) throws WdkModelException {
    return Response.ok(
      getStreamingOutput(
        Functions.mapException(
          () -> getResultSetStream(sql, queryName,
              getWdkModel().getAppDb().getDataSource(),
              FETCH_SIZE, new ResultSetToJsonConverter()),
          e -> new WdkModelException(errorMsgOnFail + " SQL: " + sql, e)
        )
      )
    ).build();
  }
  
  @GET
  @Path("ProfileSetNames/{datasetPresenterId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getProfileSetNames(
      @PathParam("datasetPresenterId") String datasetPresenterId,
      @DefaultValue("none") @QueryParam("sourceId") String sourceId)
          throws WdkModelException {
    return getStreamingResponse(getSql("ProfileSetNames", datasetPresenterId, sourceId, null, null, null, 0),
        "getProfileSetNames", "Failed running SQL to fetch profile set names.");
  }

  //TODO decide how many of these we need, what they'll be called
  //possible we'll want a separate one to handle cases which provide their own sql?
  //how to handle the transcription summary, pathway genera?
  //currently this should handle profile and profilebyec 
  @POST
  @Path("PlotData/{sourceId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getPlotData(
	@PathParam("sourceId") String sourceId, String body)
          throws WdkModelException {
    JSONObject jsonObj = new JSONObject(body);
    String sqlName = jsonObj.getString("sqlName");
    String plotDataSql = new String();

    JSONArray profileSets = jsonObj.getJSONArray("profileSets");
    for (int i = 0; i < profileSets.length(); i++) {
      JSONObject profileSet = profileSets.getJSONObject(i);
      if (profileSet.has("profileSetName")) {
        String profileSetName = profileSet.getString("profileSetName");
        String profileType = profileSet.getString("profileType");
        if (profileSet.has("facet") || profileSet.has("xAxis")) {
          String facet = profileSet.getString("facet");
          String xAxis = profileSet.getString("xAxis");
          if (plotDataSql.isEmpty()) {
            plotDataSql = getSql(sqlName, profileSetName, profileType, facet, xAxis, sourceId, i);
          } else {
            plotDataSql = plotDataSql + " UNION " + getSql(sqlName, profileSetName, profileType, facet, xAxis, sourceId, i);
          }
        } else {
          if (plotDataSql.isEmpty()) {
            plotDataSql = getSql(sqlName, profileSetName, profileType, sourceId, null, null, i);
          } else {
            plotDataSql = plotDataSql + " UNION " + getSql(sqlName, profileSetName, profileType, sourceId, null, null, i);
          }
        }
      } else {
        String sourceIdValueQuery = profileSet.getString("sourceIdValueQuery");
        String N = profileSet.getString("N");
        if (profileSet.has("idOverride")) {
          if (plotDataSql.isEmpty()) {
            plotDataSql = getSql(sqlName, sourceIdValueQuery, profileSet.getString("idOverride"), N, null, null, i);
          } else {
            plotDataSql = plotDataSql + " UNION " + getSql(sqlName, sourceIdValueQuery, profileSet.getString("idOverride"), N, null, null, i);
          }
        } else {
          if (plotDataSql.isEmpty()) {
            plotDataSql = getSql(sqlName, sourceIdValueQuery, sourceId, N, null, null, i);
          } else {
            plotDataSql = plotDataSql + " UNION " + getSql(sqlName, sourceIdValueQuery, sourceId, N, null, null, i);
          }
        }
      }
    }
    plotDataSql = plotDataSql + " order by profile_order, element_order";

    return getStreamingResponse(plotDataSql,
        "plotData", "Failed running SQL to fetch plot data.");
  }

  //TODO refactor all sql into xml files like we do for jbrowse
  private static String getProfileSetSql(String profileSetName, String profileType, String sourceId, int order) {

    return " select " + order + " as profile_order, name, value, samplenames.profile_set_name, samplenames.profile_type, samplenames.element_order " +
    " from (select rownum as element_order, ps.*  " +
    "                 FROM (SELECT protocol_app_node_name AS name, study_name as profile_set_name, profile_type " +
    "                     FROM  apidbtuning.ProfileSamples " +
    "                     WHERE  study_name = '" + profileSetName + "'" +
    "                     AND profile_type = '" + profileType + "'" +
    "                     ORDER  BY node_order_num) ps) samplenames, " +
    "     (select distinct rownum as element_order " +
    "                     , trim(regexp_substr(t.profile_as_string, '[^' || CHR(9) || ']+', 1, levels.column_value))  as value, profile_set_name, profile_type " +
    "                      from (SELECT profile_AS_STRING, profile_set_name, profile_type " +
    "                             FROM apidbtuning.Profile  p " +
    "                             WHERE p.source_id  = '" + sourceId + "' " +
    "                             AND p.profile_set_name = '" + profileSetName + "'" +
    "                             AND p.profile_type = '" + profileType + "') t " +
    "                     , table(cast(multiset(select level from dual connect by  level <= length (regexp_replace(t.profile_as_string, '[^' || CHR(9) || ']+'))  + 1) as sys.OdciNumberList)) levels) samplevalues " +
    " where samplenames.element_order = samplevalues.element_order " +
    " and value is not null";
  }

  private static String getProfileSetByECSql(String profileSetName, String profileType, String sourceId, int order) {

    return " select " + order + " as profile_order, name, value, samplenames.profile_set_name, samplenames.profile_type, samplenames.element_order " +
    " from (select  rownum as element_order, ps.* FROM (" +
          " SELECT protocol_app_node_name AS name, study_name as profile_set_name, profile_type" +
          " FROM  apidbtuning.ProfileSamples" +
          " WHERE  study_name = '" + profileSetName + "'" +
          " AND profile_type = '" + profileType + "'" +
          " ORDER  BY node_order_num) ps) samplenames, " +
    "     (select distinct rownum as element_order " +
    "                     , trim(regexp_substr(t.profile_as_string, '[^' || CHR(9) || ']+', 1, levels.column_value))  as value, profile_set_name, profile_type " +
    "                      from(select p.source_id, ec.ec_number, p.profile_as_string, p.profile_set_name, p.profile_type" +
                " from apidbtuning.profile p," +
                " (SELECT DISTINCT ta.gene_source_id, ec.ec_number" +
                "  FROM  dots.aaSequenceEnzymeClass asec" +
                "      , sres.enzymeClass ec" +
                "      , ApidbTuning.TranscriptAttributes ta" +
                "  WHERE ta.aa_sequence_id = asec.aa_sequence_id" +
                "  AND asec.enzyme_class_id = ec.enzyme_class_id" +                    "  AND ec.ec_number LIKE REPLACE(REPLACE(REPLACE(REPLACE(lower('" + sourceId + "'),' ',''),'-', '%'),'*','%'),'any','%')" +
                " ) ec" +
                " WHERE p.profile_set_name = '" + profileSetName + "'" +
                " AND p.profile_type = '" + profileType + "'" +
                " AND p.source_id = ec.gene_source_id) t " +
    "                     , table(cast(multiset(select level from dual connect by  level <= length (regexp_replace(t.profile_as_string, '[^' || CHR(9) || ']+'))  + 1) as sys.OdciNumberList)) levels) samplevalues " +
    " where samplenames.element_order = samplevalues.element_order " +
    " and value is not null";
  }

  private static String getProfileSetWithMetadataSql(String profileSetName, String profileType, String facet, String xAxis, String sourceId, int order) {

    return " select " + order + " as profile_order, name, value, samplenames.profile_set_name, samplenames.profile_type, samplenames.element_order, samplenames.facet, samplenames.contxaxis " +
    " from (select  rownum as element_order, ps.NAME, ps.FACET" +
          "       , ps.CONTXAXIS, ps.profile_type, ps.profile_set_name FROM (" +
          "  SELECT distinct s.protocol_app_node_name AS name" +
          "       , s.NODE_ORDER_NUM, m1.string_value as facet" +
          "       , m2.string_value as contXAxis" +
          "       , s.profile_type, s.study_name profile_set_name" +
          "  FROM  apidbtuning.ProfileSamples s" +
          "      , apidbtuning.metadata m1" +
          "      , apidbtuning.metadata m2" +
          "  WHERE  s.study_name = '" + profileSetName + "'" +
          "  AND s.profile_type = '" + profileType + "'" +
          "  and m1.PAN_ID(+) = s.PROTOCOL_APP_NODE_ID" +
          "  and m1.property_source_id(+) = '" + facet + "'" +
          "  and m2.PAN_ID(+) = s.PROTOCOL_APP_NODE_ID" +
          "  and m2.property_source_id(+) = '" + xAxis + "'" +
          "  ORDER  BY s.node_order_num) ps) samplenames, " +
    "     (select distinct rownum as element_order " +
    "                     , trim(regexp_substr(t.profile_as_string, '[^' || CHR(9) || ']+', 1, levels.column_value))  as value, profile_set_name, profile_type " +
    "                      from (SELECT profile_AS_STRING, profile_set_name, profile_type " +
    "                             FROM apidbtuning.Profile  p " +
    "                             WHERE p.source_id  = '" + sourceId + "' " +
    "                             AND p.profile_set_name = '" + profileSetName + "'" +
    "                             AND p.profile_type = '" + profileType + "') t " +
    "                     , table(cast(multiset(select level from dual connect by  level <= length (regexp_replace(t.profile_as_string, '[^' || CHR(9) || ']+'))  + 1) as sys.OdciNumberList)) levels) samplevalues " +
    " where samplenames.element_order = samplevalues.element_order " +
    " and value is not null";
  }

  private static String getProfileSetNamesSql(String datasetPresenterId, String sourceId) {
    return sourceId.equals("none")
      ? " select DISTINCT pt.profile_set_name, pt.profile_type" +
        " from apidbtuning.profiletype pt" +
        "   ,  (select distinct sl.study_id" +
        "       from study.studylink sl, apidbtuning.PanResults panr" +
        "       where sl.protocol_app_node_id = panr.pan_id" +
        "       and panr.result_table =" +
        "           'Results::NAFeatureDiffResult') dr" +
        "   , apidbtuning.DatasetNameTaxon dnt" +
        " where dnt.dataset_presenter_id = '" + datasetPresenterId + "'" +
        " and pt.dataset_name = dnt.name" +
        " and pt.profile_study_id = dr.study_id (+)" +
        " and dr.study_id is null"
      : " select DISTINCT pt.profile_set_name, pt.profile_type" +
        " from apidbtuning.profiletype pt, apidbtuning.profile p" +
        "   ,  (select distinct sl.study_id " +
        "       from study.studylink sl, apidbtuning.PanResults panr" +
        "       where sl.protocol_app_node_id = panr.pan_id" +
        "       and panr.result_table = 'Results::NAFeatureDiffResult') dr" +
        "  , apidbtuning.DatasetNameTaxon dnt" +
        " where dnt.dataset_presenter_id = '" + datasetPresenterId + "'" +
        " and pt.dataset_name = dnt.name" +
        " and pt.profile_study_id = dr.study_id (+)" +
        " and dr.study_id is null" +
        " and p.profile_study_id = pt.profile_study_id" +
        " and p.profile_type = pt.profile_type" +
        " and p.source_id = '" + sourceId + "'";

  }

  //TODO double check the sql here.. probably wrong
  private static String getRankedValuesSql(String sqlName, String sourceIdValueQuery, String sourceId, String N, int order) {
    String columnsToReturn = "";
    String columnsInDat = "source_id, value, name, profile_order";
    if (sqlName.equals("RankedNthSourceIdNames")) {
        columnsToReturn = "value, source_id as name, " + order + " as profile_order";
    } else if (sqlName.equals("RankedNthValues")) {
        columnsToReturn = "value, rn as name, " + order + " as profile_order";
    } else if (sqlName.equals("RankedNthRatioValues")) {
        columnsToReturn = "value, num, denom, rn as name, " + order + " as profile_order";
        columnsInDat = "source_id, value, num, denom, name, order";
    } else {
          throw new IllegalArgumentException("Unsupported named query: " + sqlName);
    }
    return " with dat as" +
           " ( " + sourceIdValueQuery + ")," +
           " ct as (select max(rownum) as m from dat)" +
           " select " + columnsToReturn + ", rn as element_order" +
           " from (select " + columnsInDat + ", rownum rn" +
           "       from (select " + columnsInDat +
           "             from dat order by value) t)" +
           " where ('" + sourceId + "' = 'ALL'" +
           "        AND (rn = 1 or rn = (select ct.m from ct)" +
           "             or mod(rn, round((select ct.m from ct)/" + N + ",0)) = 0))" +
           " OR '" + sourceId + "' = source_id";
  }

  private static String getUserDatasetsSql(String profileSetId, String sourceId) {
   return  " select pan.name, e.value, pan.node_order_num as element_order" +
           " from apidbuserdatasets.ud_protocolappnode pan" +
           "    , apidbuserdatasets.ud_nafeatureexpression e" +
           "    , apidbtuning.geneattributes ga" +
           " where pan.profile_set_id = '" + profileSetId + "'" +
           " and pan.protocol_app_node_id = e.protocol_app_node_id" +
           " and ga.na_feature_id = e.na_feature_id" +
           " and ga.source_id = '" + sourceId + "'" +
           " order by pan.node_order_num, pan.protocol_app_node_id";
  }

  //TODO figure adding antisense result to return plot ready data
  private static String getSenseAntisenseSql(String sqlName, String senseProfileSetId, String antisenseProfileSetId, String sourceId, String floor) {
    String columnsToReturn = sqlName.equals("SenseAntisenseX") ? "value as contxaxis, name" : "value";
    return " with comp as (select ps.node_order_num" +
           "                    , ps.protocol_app_node_name" +
           "                    , na.value" +
           "               from apidbtuning.ProfileSamples ps" +
           "                  , results.nafeatureexpression na" +
           "                  , apidbtuning.geneattributes ga" +
           "               where ps.study_name = '" + senseProfileSetId + "'" +
           "               and ps.profile_type = 'values'" +
           "               and ps.protocol_app_node_id = na.protocol_app_node_id" +
           "               and na.na_feature_id = ga.na_feature_id" +
           "               and ga.source_id='" + sourceId + "')" +
           "    , ref as (select ps.node_order_num" +
           "                   , ps.protocol_app_node_name" +
           "                   , na.value" +
           "              from apidbtuning.ProfileSamples ps" +
           "                 , results.nafeatureexpression na" +
           "                 , apidbtuning.geneattributes ga" +
           "              where ps.study_name = '" + senseProfileSetId + "'" +
           "              and ps.profile_type = 'values'" +
           "              and ps.protocol_app_node_id =  na.protocol_app_node_id" +
           "              and na.na_feature_id = ga.na_feature_id" +
           "              and ga.source_id='" + sourceId + "')" +
           " select " + columnsToReturn + ", ROW_NUMBER() OVER (order by NAME) as element_order" +
           " from (select ref.protocol_app_node_name || '->' || comp.protocol_app_node_name as NAME" +
           "            , round(log(2,greatest(comp.value," + floor + ") / greatest(ref.value," + floor + ")),1) as value" +
           "       from comp, ref" +
           "       where comp.protocol_app_node_name != ref.protocol_app_node_name)";
  }

//TODO the canned query does some work w genera that we'll have to replicate, probably best to do in data plotter
//TODO decide which option to take 1) the way the query wo sourceid works assuming we get a comma delim string of genera or 2) the way the query w sourceid works assuming we get some sql that makes rows out of the genera
//TODO currently leaning toward option 2, though similar to the phenotype stuff idk how we feel about sql in a service url
  private static String getPathwayGeneraSql(String generaList, String sourceId) {
    return sourceId.equals("none")
        ? " with temp as" +
          "      (select '" + generaList + "' genera  from dual)" +
          " select rownum as element_order," +
          "        trim(regexp_substr(t.genera, '[^,]+', 1, levels.column_value))  as name" +
          " from temp t," +
          "      table(cast(multiset(select level from dual connect by  level <= length (regexp_replace(t.genera, '[^,]+'))  + 1) as sys.OdciNumberList)) levels" +
          " order by genera"
        : " select case when ec.genus is null then 0 else 1 end as value" +
          "      , orgs.o as element_order" +
          " from (select distinct genus" +
          "       from apidb.ecnumbergenus" +
          "       where ec_number LIKE REPLACE(REPLACE(REPLACE(REPLACE(lower('" + sourceId + "'),' ',''),'-', '%'),'*','%'),'any','%')" +
          "  UNION" +
          " select distinct 'Plasmodium' as genus" +
          " from dots.AaSequenceEnzymeClass asec, sres.EnzymeClass ec" +
          " where ec.enzyme_class_id = asec.enzyme_class_id" +
          " and ec.ec_number LIKE REPLACE(REPLACE(REPLACE(REPLACE(lower('<<Id>>'),' ',''),'-', '%'),'*','%'),'any','%')" +
          " ) ec," +
          "  (" + generaList + ") orgs" +
          " where orgs.genus = ec.genus (+)" +
          " order by orgs.o asc";
  }

  //some of these nameless params may be null.. consider better ways to do this
  private static String getSql(String sqlName, String param1, String param2, String param3, String param4, String param5, int order) {
    switch(sqlName) {
      case "ProfileSetNames":
        return getProfileSetNamesSql(param1, param2);
      case "Profile":
        return getProfileSetSql(param1, param2, param3, order);
      case "ProfileWithMetadata":
        return getProfileSetWithMetadataSql(param1, param2, param3, param4, param5, order);
      case "RankedNthSourceIdNames":
        return getRankedValuesSql(sqlName, param1, param2, param3, order);
      case "RankedNthValues":
        return getRankedValuesSql(sqlName, param1, param2, param3, order);
      case "RankedNthRatioValues":
        return getRankedValuesSql(sqlName, param1, param2, param3, order);
      case "UserDatasets":
        return getUserDatasetsSql(param1, param2);
      case "SenseAntisense":
        return getSenseAntisenseSql(sqlName, param1, param2, param3, param4);
      case "ProfileByEC":
        return getProfileSetByECSql(param1, param2, param3, order);
      case "PathwayGenera":
        return getPathwayGeneraSql(param1, param2);
      default:
          throw new IllegalArgumentException("Unsupported named query: " + sqlName);
    }
  }

}
