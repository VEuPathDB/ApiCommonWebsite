package org.apidb.apicommon.service.services.dataPlotter;

import static org.gusdb.fgputil.FormatUtil.NL;
import static org.gusdb.fgputil.FormatUtil.enumValuesAsString;
import static org.gusdb.fgputil.db.stream.ResultSetInputStream.getResultSetStream;
import static org.gusdb.fgputil.functional.Functions.executesWithoutException;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.Timer;
import org.gusdb.fgputil.db.ResultSetColumnInfo;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.db.stream.ResultSetInputStream.ResultSetRowConverter;
import org.gusdb.fgputil.db.stream.ResultSetToNdJsonConverter;
import org.gusdb.fgputil.functional.Functions;
import org.gusdb.fgputil.runtime.GusHome;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.service.service.AbstractWdkService;


@Path("/profileSet")
public class ProfileSetService extends AbstractWdkService {

  @SuppressWarnings("unused")
  private static final Logger LOG = Logger.getLogger(ProfileSetService.class);

  private static final int FETCH_SIZE = 10000;

  private enum Mode {
    PrintSql,
    RunQuery;
  }

  private enum DataType {
    ProfileSetNames,
    Profile,
    ElementNames,
    PhenotypeRankedNthNames,
    PhenotypeRankedNthSourceIdNames,
    PhenotypeRankedNthValues,
    RankedNthRatioValues,
    UserDatasets,
    SenseAntisenseX,
    SenseAntisenseY,
    ProfileByEC,
    PathwayGenera;
  }

  private Response getStreamingResponse(String sql, String queryName, String errorMsgOnFail) throws WdkModelException {
    return Response.ok(
      getStreamingOutput(
        Functions.mapException(
          () -> getResultSetStream(sql, queryName,
              getWdkModel().getAppDb().getDataSource(),
              FETCH_SIZE, new ResultSetToNdJsonConverter()),
          e -> new WdkModelException(errorMsgOnFail + " SQL: " + sql, e)
        )
      )
    ).build();
  }

  public static void main(String[] args) throws WdkModelException, IOException {
    if (args.length != 8 ||
        !executesWithoutException(Mode::valueOf, args[0]) ||
        !executesWithoutException(DataType::valueOf, args[2])) {
      System.err.println(NL +
          "USAGE: fgpJava " + ProfileSetService.class.getName() +
          " <mode> <projectId> <dataType> <param1> <param2> <param3> <param4> <outputFile>" + NL + NL +
          "Notes:" + NL +
          "  mode must be one of " + enumValuesAsString(Mode.values()) + NL +
          "  dataType must be one of " + enumValuesAsString(DataType.values()) + NL +
          "  valid params for each dataType are " + NL + 
          "    ProfileSetNames: <datasetPresenterId> <sourceId> null null" + NL +
          "    ElementNames: <profileSetName> <profileType> <facet> <xAxis>" + NL +
          "    Profile: <sourceId> <profileSetName> <profileType> null" + NL +
          "    ProfileByEC: <sourceId> <profileSetName> <profileType> null" + NL +
          "    PhenotypeRankedNthSourceIdNames: <sourceIdValueQuery> <sourceId> <N> null" + NL +
          "    PhenotypeRankedNthNames: <sourceIdValueQuery> <sourceId> <N> null" + NL +
          "    PhenotypeRankedNthValues: <sourceIdValueQuery> <sourceId> <N> null" + NL +
          "    RankedNthRatioValues: <sourceIdValueQuery> <sourceId> <N> null" + NL +
          "    SenseAntisenseX: <profileSetId> <sourceId> <floor> null" + NL +
          "    SenseAntisenseY: <profileSetId> <sourceId> <floor> null" + NL +
          "    UserDatasets: <profileSetId> <sourceId> null null" + NL +
          "    PathwayGenera: <generaList> <sourceId> null null" + NL + NL);
      System.exit(1);
    }

    String sql = getSql(DataType.valueOf(args[2]), args[3], args[4], args[5], args[6]);
    System.out.println("Using SQL: " + sql);

    if (Mode.valueOf(args[0]).equals(Mode.RunQuery)) {
      try (WdkModel wdkModel = WdkModel.construct(args[1], GusHome.getGusHome());
           BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(args[7]), 10240)) {
        Timer t = new Timer();
        ResultSetRowConverter rowConverter = new ResultSetToNdJsonConverter();
        new SQLRunner(wdkModel.getAppDb().getDataSource(), sql)
          .executeQuery(rs -> {
            try {
              ResultSetColumnInfo metadata = new ResultSetColumnInfo(rs);
              out.write(rowConverter.getHeader());
              if (rs.next()) {
                out.write(rowConverter.getRow(rs, metadata));
              }
              while (rs.next()) {
                out.write(rowConverter.getRowDelimiter());
                out.write(rowConverter.getRow(rs, metadata));
              }
              out.write(rowConverter.getFooter());
              return null;
            }
            catch (IOException e) {
              throw new RuntimeException(e);
            }
          });
        System.out.println("Wrote file in " + t.getElapsedString());
      }
    }
  }

  @GET
  @Path("ProfileSetNames/{datasetPresenterId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getProfileSetNames(
      @PathParam("datasetPresenterId") String datasetPresenterId,
      @DefaultValue("none") @QueryParam("sourceId") String sourceId)
          throws WdkModelException {
    return getStreamingResponse(getSql(DataType.ProfileSetNames, datasetPresenterId, sourceId, null, null),
        "getProfileSetNames", "Failed running SQL to fetch profile set names.");
  }

  @GET
  @Path("ElementNames/{profileSetName}/{profileType}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getElementNames(
      @PathParam("profileSetName") String profileSetName,
      @PathParam("profileType") String profileType,
      @DefaultValue("none") @QueryParam("facet") String facet,
      @DefaultValue("none") @QueryParam("xAxis") String xAxis)
          throws WdkModelException {
    return getStreamingResponse(getSql(DataType.ElementNames, profileSetName, profileType, facet, xAxis),
        "getElementNames", "Failed running SQL to fetch element names.");
  }

  @GET
  @Path("Profile/{sourceId}/{profileSetName}/{profileType}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getProfile(
      @PathParam("sourceId") String sourceId,
      @PathParam("profileSetName") String profileSetName,
      @PathParam("profileType") String profileType)
          throws WdkModelException {
    return getStreamingResponse(getSql(DataType.Profile, sourceId, profileSetName, profileType, null),
        "getProfile", "Failed running SQL to fetch profiles.");
  }

  @GET  
  @Path("ProfileByEC/{sourceId}/{profileSetName}/{profileType}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getProfileByEC(
      @PathParam("sourceId") String sourceId,
      @PathParam("profileSetName") String profileSetName,
      @PathParam("profileType") String profileType)
          throws WdkModelException {
    return getStreamingResponse(getSql(DataType.ProfileByEC, sourceId, profileSetName, profileType, null),
        "getProfileByEC", "Failed running SQL to fetch profiles.");
  }

  //TODO sourceIdValueQuery means passing sql in the service url.. :(
  @GET
  @Path("PhenotypeRankedNthNames/{sourceIdValueQuery}/{sourceId}/{N}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getPhenotypeRankedNthNames(
      @PathParam("sourceIdValueQuery") String sourceIdValueQuery,
      @PathParam("sourceId") String sourceId,
      @PathParam("N") String N)
          throws WdkModelException {
    return getStreamingResponse(getSql(DataType.PhenotypeRankedNthNames, sourceIdValueQuery, sourceId, N, null),
        "getPhenotypeRankedNthNames", "Failed running SQL to fetch phenotype ranked names.");
  }

  @GET
  @Path("PhenotypeRankedNthSourceIdNames/{sourceIdValueQuery}/{sourceId}/{N}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getPhenotypeRankedNthSourceIdNames(
      @PathParam("sourceIdValueQuery") String sourceIdValueQuery,
      @PathParam("sourceId") String sourceId,
      @PathParam("N") String N)
          throws WdkModelException {
    return getStreamingResponse(getSql(DataType.PhenotypeRankedNthSourceIdNames, sourceIdValueQuery, sourceId, N, null),
        "getPhenotypeRankedNthSourceIdNames", "Failed running SQL to fetch phenotype ranked source id names.");
  }

  @GET
  @Path("PhenotypeRankedNthValues/{sourceIdValueQuery}/{sourceId}/{N}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getPhenotypeRankedNthValues(
      @PathParam("sourceIdValueQuery") String sourceIdValueQuery,
      @PathParam("sourceId") String sourceId,
      @PathParam("N") String N)
          throws WdkModelException {
    return getStreamingResponse(getSql(DataType.PhenotypeRankedNthValues, sourceIdValueQuery, sourceId, N, null),
        "getPhenotypeRankedNthValues", "Failed running SQL to fetch phenotype ranked values.");
  }

  @GET
  @Path("RankedNthRatioValues/{sourceIdValueQuery}/{sourceId}/{N}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getRankedNthRatioValues(
      @PathParam("sourceIdValueQuery") String sourceIdValueQuery,
      @PathParam("sourceId") String sourceId,
      @PathParam("N") String N)
          throws WdkModelException {
    return getStreamingResponse(getSql(DataType.RankedNthRatioValues, sourceIdValueQuery, sourceId, N, null),
        "getRankedNthRatioValues", "Failed running SQL to fetch ranked values.");
  }

  @GET
  @Path("UserDatasets/{profileSetId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUserDatasets(
      @PathParam("profileSetId") String profileSetId,
      @DefaultValue("none") @QueryParam("sourceId") String sourceId)
          throws WdkModelException {
    return getStreamingResponse(getSql(DataType.UserDatasets, profileSetId, sourceId, null, null),
        "getUserDatasets", "Failed running SQL to fetch user datasets.");
  }

  //TODO consider changing profileSetId -> profileSetName for consistency
  @GET
  @Path("SenseAntisenseX/{profileSetId}/{sourceId}/{floor}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSenseAntisenseX(
      @PathParam("profileSetId") String profileSetId,
      @PathParam("sourceId") String sourceId,
      @PathParam("floor") String floor)
          throws WdkModelException {
    return getStreamingResponse(getSql(DataType.SenseAntisenseX, profileSetId, sourceId, floor, null),
        "getSenseAntisenseX", "Failed running SQL to fetch sense antisense x axis.");
  }

  @GET
  @Path("SenseAntisenseY/{profileSetId}/{sourceId}/{floor}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSenseAntisenseY(
      @PathParam("profileSetId") String profileSetId,
      @PathParam("sourceId") String sourceId,
      @PathParam("floor") String floor)
          throws WdkModelException {
    return getStreamingResponse(getSql(DataType.SenseAntisenseY, profileSetId, sourceId, floor, null),
        "getSenseAntisenseY", "Failed running SQL to fetch sense antisense y axis.");
  }

  @GET
  @Path("PathwayGenera/{generaList}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getPathwayGenera(
      @PathParam("generaList") String generaList,
      @DefaultValue("none") @QueryParam("sourceId") String sourceId)
          throws WdkModelException {
    return getStreamingResponse(getSql(DataType.PathwayGenera, generaList, sourceId, null, null),
        "getPathwayGenera", "Failed running SQL to fetch pathway genera.");
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
 
  private static String getElementNamesSql(String profileSetName, String profileType, String facet, String xAxis) {
    return facet.equals("none") || xAxis.equals("none")
        ? " select  rownum as element_order, ps.* FROM (" +
          " SELECT protocol_app_node_name AS name" +
          " FROM  apidbtuning.ProfileSamples" +
          " WHERE  study_name            = '" + profileSetName + "'" +
          " AND profile_type = '" + profileType + "'" +
          " ORDER  BY node_order_num" +
          ") ps"
        : " select  rownum as element_order, ps.NAME, ps.FACET" +
          "       , ps.CONTXAXIS FROM (" +
          "  SELECT distinct s.protocol_app_node_name AS name" +
          "       , s.NODE_ORDER_NUM, m1.string_value as facet" +
          "       , m2.string_value as contXAxis" +
          "  FROM  apidbtuning.ProfileSamples s" +
          "      , apidbtuning.metadata m1" +
          "      , apidbtuning.metadata m2" +
          "  WHERE  s.study_name = '" + profileSetName + "'" +
          "  AND s.profile_type = '" + profileType + "'" +
          "  and m1.PAN_ID(+) = s.PROTOCOL_APP_NODE_ID" +
          "  and m1.property_source_id(+) = '" + facet + "'" + 
          "  and m2.PAN_ID(+) = s.PROTOCOL_APP_NODE_ID" +
          "  and m2.property_source_id(+) = '" + xAxis + "'" +
          "  ORDER  BY s.node_order_num" +
          " ) ps";
 
  }

  private static String getProfileSql(DataType dataType, String sourceId, String profileSetName, String profileType) {
    String profile = "";
    if (dataType == DataType.Profile) {
      profile = " SELECT profile_AS_STRING" +
                " FROM apidbtuning.Profile  p" +
                " WHERE p.source_id  = '" + sourceId + "'" +
                " AND p.profile_set_name  = '" + profileSetName + "'" +
                " AND p.profile_type = '" + profileType + "'";
    } else if (dataType == DataType.ProfileByEC) {
      profile = " select p.source_id, ec.ec_number, p.profile_as_string" +
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
                " AND p.source_id = ec.gene_source_id";
    } else {
      throw new IllegalArgumentException("Unsupported data type: " + dataType);
    } 

    return " with temp as (" + profile + ")" +
           " select distinct rownum as element_order" +
           "      , trim(regexp_substr(t.profile_as_string, '[^' || CHR(9) || ']+', 1, levels.column_value))  as value" +
           " from temp t" +
           "    , table(cast(multiset(select level from dual connect by  level <= length (regexp_replace(t.profile_as_string, '[^' || CHR(9) || ']+'))  + 1) as sys.OdciNumberList)) levels";

  }

  private static String getRankedValuesSql(DataType dataType, String sourceIdValueQuery, String sourceId, String N) {
    String columnsToReturn = "";
    String columnsInDat = "source_id, value";
    if (dataType == DataType.PhenotypeRankedNthNames) {
        columnsToReturn = "rn as name";
    } else if (dataType == DataType.PhenotypeRankedNthSourceIdNames) {
        columnsToReturn = "source_id as name";
    } else if (dataType == DataType.PhenotypeRankedNthValues) {
        columnsToReturn = "value";
    } else if (dataType == DataType.RankedNthRatioValues) {
        columnsToReturn = "value, num, denom";
        columnsInDat = "source_id, value, num, denom";
    } else {
          throw new IllegalArgumentException("Unsupported data type: " + dataType);
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
    return sourceId.equals("none")
        ? " select name, node_order_num as element_order" +
          " from apidbuserdatasets.ud_protocolappnode" +
          " where profile_set_id = '" + profileSetId + "'" +
          " order by node_order_num, protocol_app_node_id"
        : " select e.value, pan.node_order_num as element_order" +
          " from apidbuserdatasets.ud_protocolappnode pan" +
          "    , apidbuserdatasets.ud_nafeatureexpression e" +
          "    , apidbtuning.geneattributes ga" +
          " where pan.profile_set_id = '" + profileSetId + "'" +
          " and pan.protocol_app_node_id = e.protocol_app_node_id" +
          " and ga.na_feature_id = e.na_feature_id" +
          " and ga.source_id = '" + sourceId + "'" +
          " order by pan.node_order_num, pan.protocol_app_node_id";
  }

  private static String getSenseAntisenseSql(DataType dataType, String profileSetId, String sourceId, String floor) {
    String columnsToReturn = dataType.equals(DataType.SenseAntisenseX) ? "value as contxaxis, name" : "value";
    return " with comp as (select ps.node_order_num" +
           "                    , ps.protocol_app_node_name" +
           "                    , na.value" +
           "               from apidbtuning.ProfileSamples ps" +
           "                  , results.nafeatureexpression na" +
           "                  , apidbtuning.geneattributes ga" +
           "               where ps.study_name = '" + profileSetId + "'" +
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
           "              where ps.study_name = '" + profileSetId + "'" +
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
  private static String getSql(DataType dataType, String param1, String param2, String param3, String param4) {
    switch(dataType) {
      case ProfileSetNames:
        return getProfileSetNamesSql(param1, param2);
      case Profile:
        return getProfileSql(dataType, param1, param2, param3);
      case ElementNames:
        return getElementNamesSql(param1, param2, param3, param4);
      case PhenotypeRankedNthNames:
        return getRankedValuesSql(dataType, param1, param2, param3);
      case PhenotypeRankedNthSourceIdNames:
        return getRankedValuesSql(dataType, param1, param2, param3);
      case PhenotypeRankedNthValues:
        return getRankedValuesSql(dataType, param1, param2, param3);
      case RankedNthRatioValues:
        return getRankedValuesSql(dataType, param1, param2, param3);
      case UserDatasets:
        return getUserDatasetsSql(param1, param2);
      case SenseAntisenseX:
        return getSenseAntisenseSql(dataType, param1, param2, param3);
      case SenseAntisenseY:
        return getSenseAntisenseSql(dataType, param1, param2, param3);
      case ProfileByEC:
        return getProfileSql(dataType, param1, param2, param3);
      case PathwayGenera:
        return getPathwayGeneraSql(param1, param2);
      default:
          throw new IllegalArgumentException("Unsupported data type: " + dataType);
    }
  }

}
