package org.apidb.apicommon.service.services.jbrowse;

import static org.gusdb.wdk.service.FileRanges.getFileChunkResponse;
import static org.gusdb.wdk.service.FileRanges.parseRangeHeaderValue;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apidb.apicommon.model.JBrowseQueries;
import org.apidb.apicommon.model.JBrowseQueries.Category;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.service.service.AbstractWdkService;
import org.json.JSONArray;
import org.json.JSONObject;

@Path("/jbrowse")
public class JBrowseService extends AbstractWdkService {

    @GET
    @Path("stats/global")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseGlobalStats(@SuppressWarnings("unused") @QueryParam("feature") String feature) {
        return Response.ok(new JSONObject().put("featureDensity", 0.0002).toString()).build();
    }

    @GET
    @Path("stats/regionFeatureDensities/{refseq_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseRegionFeatureDensities(@PathParam("refseq_name") String refseqName, 
                                                     @Context UriInfo uriInfo,
                                                     @QueryParam("feature") String feature,
                                                     @QueryParam("start") String start,
                                                     @QueryParam("end") String end) {
        return featuresAndRegionStats(refseqName, uriInfo, feature, Integer.valueOf(start), Integer.valueOf(end));
    }

    @GET
    @Path("features/{refseq_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseFeatures(@PathParam("refseq_name") String refseqName, 
                                       @Context UriInfo uriInfo,
                                       @QueryParam("feature") String feature,
                                       @QueryParam("start") String start,
                                       @QueryParam("end") String end) {
        return featuresAndRegionStats(refseqName, uriInfo, feature, Integer.valueOf(start), Integer.valueOf(end));
    }

    @GET
    @Path("dnaseq/{organismAbbrev}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseDNASeqTracks(@PathParam("organismAbbrev") String organismAbbrev) throws IOException {

        String gusHome = getWdkModel().getGusHome();
        String projectId = getWdkModel().getProjectId();

        List<String> command = new ArrayList<String>();
        command.add(gusHome + "/bin/jbrowseDNASeqTracks");
        command.add(organismAbbrev);
        command.add(projectId);

        return responseFromCommand(command);
    }

    @GET
    @Path("rnaseqJunctions/{organismAbbrev}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseRNASeqJunctionTracks(@PathParam("organismAbbrev") String organismAbbrev, @DefaultValue("0") @QueryParam("isApollo") String isApollo)  throws IOException {

        String gusHome = getWdkModel().getGusHome();
        String projectId = getWdkModel().getProjectId();

        List<String> command = new ArrayList<String>();
        command.add(gusHome + "/bin/jbrowseRNASeqJunctionTracks");
        command.add(organismAbbrev);
        command.add(projectId);
        command.add(isApollo);

        return responseFromCommand(command);
    }

    @GET
    @Path("{tracks}/{organismAbbrev}/aa/trackList.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseAaTracks(@PathParam("organismAbbrev") String organismAbbrev, @PathParam("tracks") String tracks)  throws IOException {

        String gusHome = getWdkModel().getGusHome();
        String projectId = getWdkModel().getProjectId();
        boolean isPbrowse = true;

        List<String> command = new ArrayList<String>();
        command.add(gusHome + "/bin/jbrowseTracks");
        command.add(organismAbbrev);
        command.add(projectId);
        command.add(String.valueOf(isPbrowse));
        command.add(tracks);

        return responseFromCommand(command);
    }

    @GET
    @Path("{tracks}/{organismAbbrev}/trackList.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseTracks(@PathParam("organismAbbrev") String organismAbbrev, @PathParam("tracks") String tracks)  throws IOException {

        String gusHome = getWdkModel().getGusHome();
        String projectId = getWdkModel().getProjectId();

        boolean isPbrowse = false;

        List<String> command = new ArrayList<String>();
        command.add(gusHome + "/bin/jbrowseTracks");
        command.add(organismAbbrev);
        command.add(projectId);
        command.add(String.valueOf(isPbrowse));
        command.add(tracks);

        return responseFromCommand(command);
    }

    @GET
    @Path("organismSpecific/{organismAbbrev}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseOrganismSpecificTracks(@PathParam("organismAbbrev") String organismAbbrev)  throws IOException {

        String gusHome = getWdkModel().getGusHome();
        String projectId = getWdkModel().getProjectId();

        List<String> command = new ArrayList<String>();
        command.add(gusHome + "/bin/jbrowseOrganismSpecificTracks");
        command.add(organismAbbrev);
        command.add(projectId);

        return responseFromCommand(command);
    }

    @GET
    @Path("organismSpecificPbrowse/{organismAbbrev}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseOrganismSpecificPbrowseTracks(@PathParam("organismAbbrev") String organismAbbrev)  throws IOException {

        String gusHome = getWdkModel().getGusHome();
        String projectId = getWdkModel().getProjectId();

        List<String> command = new ArrayList<String>();
        command.add(gusHome + "/bin/jbrowseOrganismSpecificPbrowseTracks");
        command.add(organismAbbrev);
        command.add(projectId);

        return responseFromCommand(command);
    }

    @GET
    @Path("organismList")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrganismList() throws IOException {

        String gusHome = getWdkModel().getGusHome();
        String projectId = getWdkModel().getProjectId();

        List<String> command = new ArrayList<String>();
        command.add(gusHome + "/bin/jbrowseOrganismList");
        command.add(projectId);

        return responseFromCommand(command);
    }

    @GET
    @Path("rnaseq/{organismAbbrev}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseRNASeqTracks(@PathParam("organismAbbrev") String organismAbbrev) throws IOException {

        String gusHome = getWdkModel().getGusHome();
        String projectId = getWdkModel().getProjectId();
        String buildNumber = getWdkModel().getBuildNumber();
        String webservicesDir = getWdkModel().getProperties().get("WEBSERVICEMIRROR");

        List<String> command = new ArrayList<String>();
        command.add(gusHome + "/bin/jbrowseRnaAndChipSeqTracks");
        command.add(organismAbbrev);
        command.add(projectId);
        command.add(buildNumber);
        command.add(webservicesDir);
        command.add("RNASeq");

        return responseFromCommand(command);
    }

    @GET
    @Path("chipseq/{organismAbbrev}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseChIPSeqTracks(@PathParam("organismAbbrev") String organismAbbrev) throws IOException {

        String gusHome = getWdkModel().getGusHome();
        String projectId = getWdkModel().getProjectId();
        String buildNumber = getWdkModel().getBuildNumber();
        String webservicesDir = getWdkModel().getProperties().get("WEBSERVICEMIRROR");

        List<String> command = new ArrayList<String>();
        command.add(gusHome + "/bin/jbrowseRnaAndChipSeqTracks");
        command.add(organismAbbrev);
        command.add(projectId);
        command.add(buildNumber);
        command.add(webservicesDir);
        command.add("ChIPSeq");

        return responseFromCommand(command);
    }

    @GET
    @Path("store")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getJBrowseStore(@QueryParam("data") String data,
                                    @HeaderParam("Range") String fileRange) throws WdkModelException {

        String projectId = getWdkModel().getProjectId();
        String buildNumber = getWdkModel().getBuildNumber();
        String webservicesDir = getWdkModel().getProperties().get("WEBSERVICEMIRROR");

        String path = checkPath(
            webservicesDir + "/" +
            projectId + "/" +
            "build-" + buildNumber + "/" +
            data);

        return getFileChunkResponse(Paths.get(path), parseRangeHeaderValue(fileRange));
    }

    @GET
    @Path("auxiliary")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getAuxiliaryFile(@QueryParam("data") String data,
                                    @HeaderParam("Range") String fileRange) throws WdkModelException {

        String webservicesDir = getWdkModel().getProperties().get("WEBSERVICEMIRROR");

        String auxPath = webservicesDir.replaceAll("webServices", "auxiliary");

        String path = checkPath(
            auxPath + "/" +
            data);

        return getFileChunkResponse(Paths.get(path), parseRangeHeaderValue(fileRange));
    }

    private String checkPath(String fileSystemPath) {
      // TODO: think about whether other checks belong here
      if (fileSystemPath.contains("..") || fileSystemPath.contains("$")) {
        throw new NotFoundException(formatNotFound("*"));
      }
      return fileSystemPath;
    }

    @GET
    @Path("seq/{organismAbbrev}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseRefSeqs(@PathParam("organismAbbrev") String organismAbbrev )  throws IOException {

        String gusHome = getWdkModel().getGusHome();
        String projectId = getWdkModel().getProjectId();

        List<String> command = new ArrayList<String>();
        command.add(gusHome + "/bin/jbrowseRefSeqs");
        command.add(gusHome);
        command.add(projectId);
        command.add(organismAbbrev);
        command.add("genomic");

        return responseFromCommand(command);
    }

    @GET
    @Path("aaseq/{organismAbbrev}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseAaRefSeqs(@PathParam("organismAbbrev") String organismAbbrev )  throws IOException {

        String gusHome = getWdkModel().getGusHome();
        String projectId = getWdkModel().getProjectId();

        List<String> command = new ArrayList<String>();
        command.add(gusHome + "/bin/jbrowseRefSeqs");
        command.add(gusHome);
        command.add(projectId);
        command.add(organismAbbrev);
        command.add("protein");

        return responseFromCommand(command);
    }

    @GET
    @Path("names/{organismAbbrev}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseNames(@PathParam("organismAbbrev") String organismAbbrev, @QueryParam("equals") String eq, @QueryParam("startswith") String startsWith)  throws IOException {

        String gusHome = getWdkModel().getGusHome();
        String projectId = getWdkModel().getProjectId();

        boolean isPartial = true;
        String sourceId = startsWith;

        if(eq != null && !eq.equals("")) {
            isPartial = false;
            sourceId = eq;
        }

        List<String> command = new ArrayList<String>();
        command.add(gusHome + "/bin/jbrowseNames");
        command.add(gusHome);
        command.add(projectId);
        command.add(organismAbbrev);
        command.add("genomic");
        command.add(String.valueOf(isPartial));
        command.add(sourceId);


        return responseFromCommand(command);
    }

    @GET
    @Path("aanames/{organismAbbrev}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseAaNames(@PathParam("organismAbbrev") String organismAbbrev, @QueryParam("equals") String eq, @QueryParam("startswith") String startsWith)  throws IOException {

        String gusHome = getWdkModel().getGusHome();
        String projectId = getWdkModel().getProjectId();

        boolean isPartial = true;
        String sourceId = startsWith;

        if(eq != null && !eq.equals("")) {
            isPartial = false;
            sourceId = eq;
        }

        List<String> command = new ArrayList<String>();
        command.add(gusHome + "/bin/jbrowseNames");
        command.add(gusHome);
        command.add(projectId);
        command.add(organismAbbrev);
        command.add("protein");
        command.add(String.valueOf(isPartial));
        command.add(sourceId);

        return responseFromCommand(command);
    }

    public Response featuresAndRegionStats (String refseqName, UriInfo uriInfo, String feature, Integer start, Integer end) {

        String projectId = getWdkModel().getProjectId();
        HashMap<String, String> qp = toSingleValueMap(uriInfo.getQueryParameters());
        String bulksubfeature = feature + ":bulksubfeatures";
        String featureSql = new String();
        String bulksubfeatureSql = new String();
        String seqId = new String();
        boolean isProtein = false;
        if (qp.containsKey("seqType")) {
          if (qp.get("seqType").equals("protein")) {
            isProtein = true;
          }
       }

        if (isProtein) {
          String seqIdSql = "select aa_sequence_id from apidbtuning.proteinattributes where source_id = '" + refseqName + "'";
          seqId = getSingleQueryResult(seqIdSql); 
          if (feature.equals("ReferenceSequenceAa")) {
            Integer length = end - start;
            start = (start < 0) ? 0 : start;
            featureSql = "select substr(sequence, " + start.toString() + ", " + length.toString() + ") as seq, " + start.toString() + " as startm, " + end.toString() + " as end, '" + refseqName + "' as feature_id from apidbtuning.proteinsequence where source_id = '" + refseqName + "'";
          } else {
            featureSql = JBrowseQueries.getQueryMap(projectId, Category.PROTEIN).get(feature);
            featureSql = replaceSqlMacros(featureSql, start.toString(), end.toString(), seqId, qp);
          }
          bulksubfeatureSql = JBrowseQueries.getQueryMap(projectId, Category.PROTEIN).get(bulksubfeature);
        } else {
          String seqIdSql = "select na_sequence_id from apidbtuning.genomicseqattributes where source_id = '" + refseqName + "'";
          seqId = getSingleQueryResult(seqIdSql);
          if (feature.equals("ReferenceSequence")) {
            Integer length = end - start;
            start = (start < 0) ? 0 : start;
            featureSql = "select substr(sequence, " + start.toString() + ", " + length.toString() + ") as seq, " + start.toString() + " as startm, " + end.toString() + " as end, '" + refseqName + "' as feature_id from apidbtuning.genomicsequencesequence where source_id = '" + refseqName + "'";
          } else {
            featureSql = JBrowseQueries.getQueryMap(projectId, Category.GENOME).get(feature);
            featureSql = replaceSqlMacros(featureSql, start.toString(), end.toString(), seqId, qp); 
          }
          bulksubfeatureSql = JBrowseQueries.getQueryMap(projectId, Category.GENOME).get(bulksubfeature);
        }

        System.err.println("features sql: " + featureSql);
        //get features
        Map<String, JSONObject> featureMap = new SQLRunner(getWdkModel().getAppDb().getDataSource(), featureSql).executeQuery(rs -> {
           Map<String, JSONObject> features = new HashMap<String, JSONObject>();
           ResultSetMetaData rsmd = rs.getMetaData();
           int columnCount = rsmd.getColumnCount();
     
           while (rs.next()) {
             JSONObject myFeature = new JSONObject();
             int startm = rs.getInt("STARTM");
             int featureStart = startm - 1;
             int featureEnd = rs.getInt("END");
             myFeature.put("start", featureStart);
             myFeature.put("end", featureEnd);
  
             ArrayList<String> skipMe = new ArrayList<String>() {
               {
                 add("atts");
                 add("end");
                 add("feature_id");
               }
             }; 

             for (int i = 1; i <= columnCount; i++) {
               String colLabel = rsmd.getColumnLabel(i).toLowerCase();  
               if (skipMe.contains(colLabel)) { continue; }
               myFeature.put(colLabel, rs.getString(i));          
             } 
 
             boolean hasAtts = hasColumn(rs, "ATTS");
             if (hasAtts) {
               String attrs[] = rs.getString("ATTS").split(";");
               for (int i = 0; i < attrs.length; i++) {
                 String attr[] = attrs[i].split("=");
                 if (attr.length > 1) {    
                   myFeature.put(attr[0].toLowerCase(), attr[1]);
                 }
               }
             }
             String uniqueID = Integer.toString(featureStart);
             if (hasColumn(rs, "FEATURE_ID")) {
               uniqueID = rs.getString("FEATURE_ID");
             }
             myFeature.put("uniqueID", uniqueID);
             myFeature.put("subfeatures", new JSONArray());
             features.put(uniqueID, myFeature);
           }
           return features; 
        });

        Integer minStart = -9;
        Integer maxEnd = -9;
        for (JSONObject myFeature : featureMap.values()) {
          Integer featureStart = myFeature.getInt("start");
          Integer featureEnd = myFeature.getInt("end");
          minStart = minStart == -9 || featureStart < minStart ? featureStart : minStart;
          maxEnd = maxEnd == -9 || featureEnd > maxEnd ? featureEnd : maxEnd;
        }

        if (featureMap.size() > 0 && bulksubfeatureSql != null) {
          bulksubfeatureSql = replaceSqlMacros(bulksubfeatureSql, minStart.toString(), maxEnd.toString(), seqId, qp);
          System.err.println("subfeatures sql: " + bulksubfeatureSql);
          //get subfeatures
          Map<String, JSONObject> subfeatureMap = new SQLRunner(getWdkModel().getAppDb().getDataSource(), bulksubfeatureSql).executeQuery(rs -> {
             Map<String, JSONObject> subfeatures = new HashMap<String, JSONObject>();
             ResultSetMetaData rsmd = rs.getMetaData();
             int columnCount = rsmd.getColumnCount();

             boolean hasAtts = hasColumn(rs, "ATTS");
             boolean hasTStarts = hasColumn(rs, "TSTARTS");
             boolean hasThirdTierSubfeatures = hasColumn(rs, "HAS_CHILDREN");
  
             while (rs.next()) {
               JSONObject mySubfeature = new JSONObject();
               Integer startm = rs.getInt("STARTM");
               Integer featureEnd = rs.getInt("END");
               mySubfeature.put("start", startm - 1);
               mySubfeature.put("end", featureEnd);

               String parentId = rs.getString("PARENT_ID");
               JSONObject parent = new JSONObject();
               if (featureMap.containsKey(parentId)) {
                 parent = featureMap.get(parentId);
               } else {
                 parent = subfeatures.get(parentId);
               }
    
               ArrayList<String> skipMe = new ArrayList<String>() {
                 {
                   add("atts");
                   add("feature_id");
                   add("parent_id");
                   add("end");
                 }
               };

               for (int i = 1; i <= columnCount; i++) {
                 String colLabel = rsmd.getColumnLabel(i).toLowerCase(); 
                 if (skipMe.contains(colLabel)) { continue; }
                 mySubfeature.put(colLabel, rs.getString(i));
               }

               if (hasAtts) {
                 String attrs[] = rs.getString("ATTS").split(";");
                 for (int i = 0; i < attrs.length; i++) {
                   String attr[] = attrs[i].split("=");
                   if (attr.length > 1) {
                     mySubfeature.put(attr[0].toLowerCase(), attr[1]);
                   }
                 }
               }
   
               if (hasTStarts) {
                 String[] tstarts = rs.getString("TSTARTS").split(",");
                 String[] blocksizes = rs.getString("BLOCKSIZES").split(",");
                 for (int i = 0; i < tstarts.length; i++) {
                   int tstart = Integer.valueOf(tstarts[i]) - 1;
                   int tend = tstart + Integer.valueOf(blocksizes[i]);
    
                   JSONObject tmp = new JSONObject(mySubfeature, JSONObject.getNames(mySubfeature));
                   tmp.put("feature_id", rs.getString("FEATURE_ID") + "_" + i);
                   tmp.put("name", rs.getString("NAME") + "_" + i);
                   tmp.put("start", tstart);
                   tmp.put("end", tend);
                   if (parent != null) {
                     parent.append("subfeatures", tmp);
                     featureMap.put(parentId, parent);  
                   }
                 }
  
               } else {
                 if (hasThirdTierSubfeatures) {
                   boolean hasChildren = rs.getInt("HAS_CHILDREN") == 1 ? true : false;
                   if (hasChildren) {
                     String uniqueID = rs.getString("FEATURE_ID");
                     mySubfeature.put("uniqueID", uniqueID);
                     mySubfeature.put("subfeatures", new JSONArray());
                     mySubfeature.put("parentId", parentId);
                     subfeatures.put(uniqueID, mySubfeature);
                   } else {
                     parent.append("subfeatures", mySubfeature);
                     if (featureMap.containsKey(parentId)) {
                       featureMap.put(parentId, parent);
                     } else {
                       subfeatures.put(parentId, parent);
                     }
                   }
                 }
               }
 
            }
            return subfeatures;
          });
          
          for (String key : subfeatureMap.keySet()) {
            JSONObject subfeature = subfeatureMap.get(key);
            String parentId = subfeature.getString("parentId");
            JSONObject parent = featureMap.get(parentId);
            //subfeature.put(parentId, JSONObject.NULL);
            if (parent != null) { 
              parent.append("subfeatures", subfeature);
              featureMap.put(parentId, parent);
            }
          }
        }

        JSONObject features = new JSONObject();
        features.put("features", new JSONArray());
        for (String key : featureMap.keySet()) {
          JSONObject myFeature = featureMap.get(key);
          features.append("features", myFeature);
        }

        Map<Integer, Integer> bins = new HashMap<Integer, Integer>();
        if (qp.containsKey("basesPerBin")) {
          int basesPerBin = Integer.parseInt(qp.get("basesPerBin"));
          int binCount = (end - start) / basesPerBin;
          JSONArray featuresArr = features.getJSONArray("features");
          for (int i = 0; i < featuresArr.length(); i++) {
            JSONObject currentFeature = featuresArr.getJSONObject(i);
            int startBin = (currentFeature.getInt("start") - start) / basesPerBin;
            int endBin = (currentFeature.getInt("end") - start) / basesPerBin;
            int count = bins.containsKey(startBin) ? bins.get(startBin) : 0;
            bins.put(startBin, count + 1);
            if (startBin != endBin) {
              count = bins.containsKey(endBin) ? bins.get(endBin) : 0;
              bins.put(endBin, count + 1);
            }
          }

          int maxBin = 0;
          ArrayList<Integer> sortedBinValues = new ArrayList<>();
          for (int i = 0; i < binCount; i++) {
            int value = bins.containsKey(i) ? bins.get(i) : 0;
            maxBin = value > maxBin ? value : maxBin;
            sortedBinValues.add(value);
          }
           
          features.put("bins", sortedBinValues);
          Map<String, Integer> stats = new HashMap<String, Integer>();
          stats.put("basesPerBin", basesPerBin);
          stats.put("max", maxBin);
          features.put("stats", stats);
          features.remove("features");
        }        

      //TODO make sure its a stream
      return Response.ok(features.toString()).build(); 
    }

    public String getSingleQueryResult(String sql) {
        String entity = new SQLRunner(getWdkModel().getAppDb().getDataSource(), sql).executeQuery(rs -> {
          return rs.next() ? rs.getString(1) : null;
        });
        return entity;
    }

    public String replaceSqlMacros(String sql, String start, String end, String seqId, HashMap<String, String> qp) {
      sql = sql.replaceAll("\\$base_start", start);
      sql = sql.replaceAll("\\$rend", end);
      sql = sql.replaceAll("\\$dlm",";");
      sql = sql.replaceAll("\\$srcfeature_id", seqId);

      ArrayList<String> skipMe = new ArrayList<String>() { 
            { 
                add("feature"); 
                add("start"); 
                add("end");
                add("seqType"); 
            } 
      };
      
      for (Map.Entry<String, String> entry : qp.entrySet()) {
        if (skipMe.contains(entry.getKey())) { continue; }
        sql = sql.replaceAll("\\$\\$" + entry.getKey() + "\\$\\$", entry.getValue());
      }

      return sql;
    }

    public HashMap<String, String> toSingleValueMap(MultivaluedMap<String, String> mMap) {
      HashMap<String, String> svMap = new HashMap<String, String>();

      for (String key : mMap.keySet()) {
        svMap.put(key, mMap.getFirst(key));
      }
    
      return svMap; 
    }

    public boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
    ResultSetMetaData rsmd = rs.getMetaData();
    int columns = rsmd.getColumnCount();
    for (int x = 1; x <= columns; x++) {
        if (columnName.equals(rsmd.getColumnName(x))) {
            return true;
        }
    }
    return false;
}

    public Response responseFromCommand(List<String> command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        Map<String, String> env = pb.environment();
        env.put("GUS_HOME", getWdkModel().getGusHome());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        return Response.ok(getStreamingStandardOutput(p, String.join(" ", command))).build();
    }
}
