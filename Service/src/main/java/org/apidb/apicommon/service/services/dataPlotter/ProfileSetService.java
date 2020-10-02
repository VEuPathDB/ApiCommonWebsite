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
import org.apidb.apicommon.model.DataPlotterQueries;
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
  @Path("TranscriptionSummaryProfiles/{sourceId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getTranscriptionSummaryProfiles(
      @PathParam("sourceId") String sourceId)
          throws WdkModelException {
    String projectId = getWdkModel().getProjectId();
    String sql = DataPlotterQueries.getQueryMap(projectId).get("transcription_summary_profiles");
    sql = sql.replaceAll("\\$id", sourceId);
    return getStreamingResponse(sql,
        "getTranscriptionSummaryProfiles", "Failed running SQL to fetch transcription summary profile set names.");
  }
 
  @GET
  @Path("ProfileSetIds/{datasetId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getProfileSetIds(
      @PathParam("datasetId") String datasetId)
          throws WdkModelException {
    String projectId = getWdkModel().getProjectId();
    String sql = DataPlotterQueries.getQueryMap(projectId).get("profile_set_ids");
    sql = sql.replaceAll("\\$datasetId", datasetId);
    return getStreamingResponse(sql,
        "getProfileSetIds", "Failed running SQL to fetch user dataset profile set ids.");
  }
 
  @GET
  @Path("ProfileSetNames/{datasetPresenterId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getProfileSetNames(
      @PathParam("datasetPresenterId") String datasetPresenterId,
      @DefaultValue("none") @QueryParam("sourceId") String sourceId)
          throws WdkModelException {
    String projectId = getWdkModel().getProjectId();
    String sql = getSql(projectId, "ProfileSetNames", datasetPresenterId, sourceId, null, null, null, 0);
    return getStreamingResponse(sql,
        "getProfileSetNames", "Failed running SQL to fetch profile set names.");
  }

  //TODO move these sql into xml file
  @GET
  @Path("TimePointMapping/{profileSetName}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getTimePointMapping(
      @PathParam("profileSetName") String profileSetName)
          throws WdkModelException {
    String sql = "select profile_as_string from apidbtuning.profile where source_id = 'timepoint' and profile_set_name = '" + profileSetName + "'";
    return getStreamingResponse(sql,
        "getTimePointMapping", "Failed running SQL to fetch time point mapping.");
  }

  @GET
  @Path("Isotopomers/{compoundId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getIsotopomers(
      @PathParam("compoundId") String compoundId)
          throws WdkModelException {
     String sql = "WITH iso AS (" +
                  " SELECT DISTINCT chebi.isotopomer" +
                  " FROM apidb.CompoundPeaksChEBI chebi" +
                  " , study.protocolappnode pan" +
                  " , chebi.compounds c" +
	          " , apidb.CompoundMassSpecResult cms" +
                  " WHERE c.chebi_accession = '" + compoundId + "'" +
	          " AND chebi.compound_id = c.id " +
                  " AND cms.protocol_app_node_id = pan.protocol_app_node_id" + 
                  " AND cms.compound_peaks_id = chebi.compound_peaks_id)" +
	          " SELECT DISTINCT" +
	          " CASE WHEN 'C12' in (SELECT * from iso)" +
                  "  THEN nvl(isotopomer, 'C12')" +
                  "  ELSE isotopomer" +
                  "  END AS isotopomer" +
	          " FROM (SELECT * FROM iso)";
     return getStreamingResponse(sql,
				 "getIsotopomers", "Failed running SQL to fetch isotopomers.");
  }



  @GET
  @Path("CompoundPeaksIdentifier/{compoundId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getCompoundPeaksIdentifier(
      @PathParam("compoundId") String compoundId)
          throws WdkModelException {
     String sql = " SELECT Substr(source_id, Instr(source_id, '|') + 1) as CompoundPeaksIdentifier " +
                  " FROM apidbtuning.profile" +
                  " , chebi.compounds c" +
                  " WHERE c.chebi_accession = '" + compoundId + "'" +
	          " AND profile_set_name = 'Barrett_PurineStarvation [metaboliteProfiles]' " +
                  " AND source_id like '" + compoundId + "%'" + 
	          " AND profile_type ='values'" ;
     return getStreamingResponse(sql,
				 "getCompoundPeaksIdentifier", "Failed running SQL to fetch compound peaks identifier.");
  }


  @GET
  @Path("CompoundPeaksIdentifierAmoB/{compoundId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getCompoundPeaksIdentifierAmoB(
      @PathParam("compoundId") String compoundId)
          throws WdkModelException {
     String sql = " SELECT Substr(source_id, Instr(source_id, '|') + 1) as CompoundPeaksIdentifier " +
                  " FROM apidbtuning.profile" +
                  " , chebi.compounds c" +
                  " WHERE c.chebi_accession = '" + compoundId + "'" +
	          " AND profile_set_name = 'Barrett_AmphotericinB_Resistant [metaboliteProfiles]' " +
                  " AND source_id like '" + compoundId + "%'" + 
	          " AND profile_type ='values'" ;
     return getStreamingResponse(sql,
				 "getCompoundPeaksIdentifierAmoB", "Failed running SQL to fetch compound peaks identifier.");
  }




  @GET
  @Path("GutherCategory/{sourceId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getGutherCategory(
      @PathParam("sourceId") String sourceId)
          throws WdkModelException {
     String sql = " SELECT nfe.categorical_value AS cat_val" +
                  " FROM results.nafeatureexpression nfe" +
                  "    , apidbtuning.transcriptattributes ga" +
                  "    , study.protocolappnode pan" +
                  "    , study.studylink sl" +
                  "    , study.study ps" +
                  "    , study.study i" +
                  "    , sres.externaldatabaserelease r" +
                  "    , sres.externaldatabase d" +
                  " WHERE ga.gene_na_feature_id = nfe.na_feature_id" +
                  " AND nfe.protocol_app_node_id = pan.protocol_app_node_id" +
                  " AND pan.protocol_app_node_id = sl.protocol_app_node_id" +
                  " AND sl.study_id = ps.study_id" +
                  " AND ps.investigation_id = i.study_id" +
                  " AND i.external_database_release_id = r.external_database_release_id" +
                  " AND r.external_database_id = d.external_database_id" +
                  " AND d.NAME ='tbruTREU927_quantitative_massSpec_Guther_glycosomal_proteome_RSRC'" +
                  " AND ga.gene_source_id = '" + sourceId + "'";
     return getStreamingResponse(sql,
        "getGutherCategory", "Failed running SQL to fetch Guther dataset category.");
  }

  @POST
  @Path("PlotData/{sourceId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getPlotData(
	@PathParam("sourceId") String sourceId, String body)
          throws WdkModelException {
    String projectId = getWdkModel().getProjectId();
    JSONObject jsonObj = new JSONObject(body);
    String sqlName = jsonObj.getString("sqlName");
    String plotDataSql = new String();

    JSONArray profileSets = jsonObj.getJSONArray("profileSets");
    for (int i = 0; i < profileSets.length(); i++) {
      JSONObject profileSet = profileSets.getJSONObject(i);
      String id = new String();
      if (profileSet.has("idOverride")) {
        id = profileSet.getString("idOverride");
      } else {
        id = sourceId;
      }
      if (profileSet.has("profileSetName")) {
        String profileSetName = profileSet.getString("profileSetName");
        String profileType = profileSet.getString("profileType");
        if (profileSet.has("facet") || profileSet.has("xAxis")) {
          String facet = profileSet.getString("facet");
          String xAxis = profileSet.getString("xAxis");
          if (plotDataSql.isEmpty()) {
            plotDataSql = getSql(projectId, sqlName, profileSetName, profileType, facet, xAxis, id, i);
          } else {
            plotDataSql = plotDataSql + " UNION " + getSql(projectId, sqlName, profileSetName, profileType, facet, xAxis, id, i);
          }
        } else if (profileSet.has("name")) {
          String name = profileSet.getString("name");
          if (plotDataSql.isEmpty()) {
            plotDataSql = getSql(projectId, sqlName, profileSetName, profileType, id, name, null, i);
          } else {
            plotDataSql = plotDataSql + " UNION " + getSql(projectId, sqlName, profileSetName, profileType, id, name, null, i);
          }
        } else {
          if (plotDataSql.isEmpty()) {
            plotDataSql = getSql(projectId, sqlName, profileSetName, profileType, id, null, null, i);
          } else {
            plotDataSql = plotDataSql + " UNION " + getSql(projectId, sqlName, profileSetName, profileType, id, null, null, i);
          }
        }
      } else if (profileSet.has("profileSetId")) {
        String profileSetId = profileSet.getString("profileSetId");
        String name = profileSet.getString("name");
        if (plotDataSql.isEmpty()) {
            plotDataSql = getSql(projectId, sqlName, profileSetId, id, name, null, null, i);
          } else {
            plotDataSql = plotDataSql + " UNION " + getSql(projectId, sqlName, profileSetId, id, name, null, null, i);
          }
      } else if (profileSet.has("senseProfileSetId")) {
        String senseProfileSetId = profileSet.getString("senseProfileSetId");
        String antisenseProfileSetId = profileSet.getString("antisenseProfileSetId");
        String floor = profileSet.getString("floor");
        plotDataSql = getSql(projectId, sqlName, senseProfileSetId, antisenseProfileSetId, id, floor, null, i);
      } else if (profileSet.has("generaSql")) {
        String generaSql = profileSet.getString("generaSql");
        plotDataSql = getSql(projectId, sqlName, generaSql, id, null, null, null, i);
      } else {
        String sourceIdValueQuery = profileSet.getString("sourceIdValueQuery");
        String N = profileSet.getString("N");
        String name = profileSet.getString("name");
        if (plotDataSql.isEmpty()) {
          plotDataSql = getSql(projectId, sqlName, sourceIdValueQuery, id, N, name, null, i);
        } else {
          plotDataSql = plotDataSql + " UNION " + getSql(projectId, sqlName, sourceIdValueQuery, id, N, name, null, i);
        }
      }
    }
    
    if (!sqlName.equals("PathwayGenera") && !sqlName.equals("SenseAntisense")) {
      plotDataSql = plotDataSql + " order by profile_order, element_order";
    }

    return getStreamingResponse(plotDataSql,
        "plotData", "Failed running SQL to fetch plot data.");
  }

  private static String getProfileSetSql(String projectId, String profileSetName, String profileType, String sourceId, String displayName, int order) {

    String colsToReturn = order + " as profile_order, name, value, samplenames.profile_set_name, samplenames.profile_type, samplenames.element_order";
    if (displayName != null) {
      colsToReturn = order + " as profile_order, '" + displayName + "' as display_name, name, value, samplenames.profile_set_name, samplenames.profile_type, samplenames.element_order";
    }

    String sql = DataPlotterQueries.getQueryMap(projectId).get("profile_set");
    sql = sql.replaceAll("\\$colsToReturn", colsToReturn);
    sql = sql.replaceAll("\\$profileSetName", profileSetName);
    sql = sql.replaceAll("\\$profileType", profileType);
    sql = sql.replaceAll("\\$sourceId", sourceId);

    return sql;
  }

  private static String getProfileSetByECSql(String projectId, String profileSetName, String profileType, String sourceId, int order) {

    String sql = DataPlotterQueries.getQueryMap(projectId).get("profile_set_by_ec");
    sql = sql.replaceAll("\\$order", Integer.toString(order));
    sql = sql.replaceAll("\\$profileSetName", profileSetName);
    sql = sql.replaceAll("\\$profileType", profileType);
    sql = sql.replaceAll("\\$sourceId", sourceId);

    return sql;
  }

  private static String getProfileSetWithMetadataSql(String projectId, String profileSetName, String profileType, String facet, String xAxis, String sourceId, int order) {

    String sql = DataPlotterQueries.getQueryMap(projectId).get("profile_set_with_metadata");
    sql = sql.replaceAll("\\$order", Integer.toString(order));
    sql = sql.replaceAll("\\$profileSetName", profileSetName);
    sql = sql.replaceAll("\\$profileType", profileType);
    sql = sql.replaceAll("\\$sourceId", sourceId);
    sql = sql.replaceAll("\\$facet", facet);
    sql = sql.replaceAll("\\$xAxis", xAxis);

    return sql;
  }

  private static String getProfileSetNamesSql(String projectId, String datasetPresenterId, String sourceId) {
    String sql = sourceId.equals("none")
      ? DataPlotterQueries.getQueryMap(projectId).get("profile_set_names")
      : DataPlotterQueries.getQueryMap(projectId).get("profile_set_names_by_source_id");

    sql = sql.replaceAll("\\$datasetPresenterId", datasetPresenterId);
    sql = sql.replaceAll("\\$sourceId", sourceId);

    return sql;
  }

  private static String getRankedValuesSql(String projectId, String sqlName, String sourceIdValueQuery, String sourceId, String N, String name, int order) {
    String columnsToReturn = "";
    String columnsInDat = "source_id, value";
    if (sqlName.equals("RankedNthSourceIdNames")) {
        columnsToReturn = "value, source_id as name";
    } else if (sqlName.equals("RankedNthValues")) {
        columnsToReturn = "value, rn as name";
    } else if (sqlName.equals("RankedNthRatioValues")) {
        columnsToReturn = "value, num, denom, rn as name";
        columnsInDat = "source_id, value, num, denom";
    } else {
          throw new IllegalArgumentException("Unsupported named query: " + sqlName);
    }
    
    String sql = DataPlotterQueries.getQueryMap(projectId).get("ranked_values");
    sql = sql.replaceAll("\\$columnsToReturn", columnsToReturn);
    sql = sql.replaceAll("\\$columnsInDat", columnsInDat);
    sql = sql.replaceAll("\\$sourceIdValueQuery", sourceIdValueQuery);
    sql = sql.replaceAll("\\$sourceId", sourceId);
    sql = sql.replaceAll("\\$order", Integer.toString(order));
    sql = sql.replaceAll("\\$name", name);
    sql = sql.replaceAll("\\$N", N);   
 
    return sql; 
  }

  private static String getUserDatasetsSql(String projectId, String profileSetId, String sourceId, String name, int order) {

    String sql = DataPlotterQueries.getQueryMap(projectId).get("user_datasets");
    sql = sql.replaceAll("\\$order", Integer.toString(order));
    sql = sql.replaceAll("\\$name", name);
    sql = sql.replaceAll("\\$sourceId", sourceId);
    sql = sql.replaceAll("\\$profileSetId", profileSetId);
  
    return sql;
  }

  //TODO figure adding antisense result to return plot ready data
  private static String getSenseAntisenseSql(String projectId, String senseProfileSetId, String antisenseProfileSetId, String sourceId, String floor) {

    String sql = DataPlotterQueries.getQueryMap(projectId).get("sense_antisense");
    sql = sql.replaceAll("\\$floor", floor);
    sql = sql.replaceAll("\\$antisenseProfileSetId", antisenseProfileSetId);
    sql = sql.replaceAll("\\$sourceId", sourceId);
    sql = sql.replaceAll("\\$senseProfileSetId", senseProfileSetId);
 
    return sql;

  }

  private static String getPathwayGeneraSql(String projectId, String generaSql, String sourceId) {

        String sql = DataPlotterQueries.getQueryMap(projectId).get("pathway_genera");
        sql = sql.replaceAll("\\$generaSql", generaSql);
        sql = sql.replaceAll("\\$sourceId", sourceId);

        return sql;
  }

  //some of these nameless params may be null.. consider better ways to do this
  private static String getSql(String projectId, String sqlName, String param1, String param2, String param3, String param4, String param5, int order) {
    switch(sqlName) {
      case "ProfileSetNames":
        return getProfileSetNamesSql(projectId, param1, param2);
      case "Profile":
        return getProfileSetSql(projectId, param1, param2, param3, param4, order);
      case "ProfileWithMetadata":
        return getProfileSetWithMetadataSql(projectId, param1, param2, param3, param4, param5, order);
      case "RankedNthSourceIdNames":
        return getRankedValuesSql(projectId, sqlName, param1, param2, param3, param4, order);
      case "RankedNthValues":
        return getRankedValuesSql(projectId, sqlName, param1, param2, param3, param4, order);
      case "RankedNthRatioValues":
        return getRankedValuesSql(projectId, sqlName, param1, param2, param3, param4, order);
      case "UserDatasets":
        return getUserDatasetsSql(projectId, param1, param2, param3, order);
      case "SenseAntisense":
        return getSenseAntisenseSql(projectId, param1, param2, param3, param4);
      case "ProfileByEC":
        return getProfileSetByECSql(projectId, param1, param2, param3, order);
      case "PathwayGenera":
        return getPathwayGeneraSql(projectId, param1, param2);
      default:
          throw new IllegalArgumentException("Unsupported named query: " + sqlName);
    }
  }

}
