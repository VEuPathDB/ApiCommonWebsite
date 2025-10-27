package org.apidb.apicommon.service.services.jbrowse;

import static org.gusdb.fgputil.FormatUtil.NL;
import static org.gusdb.fgputil.functional.Functions.getMapFromKeys;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.sql.DataSource;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.apidb.apicommon.model.JBrowseQueries;
import org.apidb.apicommon.model.JBrowseQueries.Category;
import org.gusdb.fgputil.Range;
import org.gusdb.fgputil.Tuples.TwoTuple;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.db.runner.SQLRunnerException;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.json.JSONArray;
import org.json.JSONObject;

public class JBrowseFeatureDataFactory {

  private static final Logger LOG = Logger.getLogger(JBrowseFeatureDataFactory.class);

  private final String _projectId;
  private final DataSource _appDs;

  public JBrowseFeatureDataFactory(WdkModel wdkModel) {
    _projectId = wdkModel.getProjectId();
    _appDs = wdkModel.getAppDb().getDataSource();
  }

  public Response featuresAndRegionStats(String refseqName, UriInfo uriInfo, String feature, Long start, Long end) {

    // use only first instance of each query param
    MultivaluedMap<String,String> rawQp = uriInfo.getQueryParameters();
    Map<String, String> qp = getMapFromKeys(rawQp.keySet(), key -> rawQp.getFirst(key));

    // determine if protein vs genome request
    boolean isProtein = qp.containsKey("seqType") && qp.get("seqType").equals("protein");

    // determine if reference feature is requested
    boolean isReferenceFeature =
        (isProtein && "ReferenceSequenceAa".equals(feature)) ||
        (!isProtein && "ReferenceSequence".equals(feature));

    String seqId = (isProtein ?
                    getSequenceId("aa_sequence_id", "webready.ProteinAttributes", refseqName, "aaseq_id_from_source_id") :
                    getSequenceId("na_sequence_id", "webready.GenomicSeqAttributes", refseqName, "naseq_id_from_source_id"))
        .orElseThrow(() -> new NotFoundException("Unable to look up sequence with ref name " + refseqName + " (isProtein=" + isProtein + ")."));

    String featureSql = isProtein ?
        getFeatureSql(refseqName, feature, start, end, qp, isReferenceFeature,
            "webready.ProteinSequence", Category.PROTEIN, seqId) :
        getFeatureSql(refseqName, feature, start, end, qp, isReferenceFeature,
            "webready.GenomicSequenceSequence", Category.GENOME, seqId);

    // determine if stats vs feature data request (basesPerBin present = stats request)
    boolean isStatsRequest = qp.containsKey("basesPerBin");

    String queryName = feature;
    if(isStatsRequest) {
        queryName = queryName + "_region_stats";
    }

    // return an ok result if no exception thrown; entity returned will be one of
    if (isStatsRequest) {
      // 1. region statistics JSON object
      int basesPerBin = Integer.parseInt(qp.get("basesPerBin"));
      return Response.ok(getRegionStatsOutput(featureSql, basesPerBin, start, end, queryName).toString()).build();
    }
    else {
      // 2. feature and subfeature JSON stream
      if (isReferenceFeature) {
          return Response.ok(getFeaturesOutput(featureSql, queryName)).build();
      }
      else {
        Optional<String> bulkSubfeatureSql = getBulkSubfeatureSql(feature, seqId, featureSql, qp, isProtein ? Category.PROTEIN : Category.GENOME);
        return Response.ok(
          bulkSubfeatureSql.isPresent() ?
          getFeaturesOutput(featureSql, bulkSubfeatureSql.get(), queryName) :
          getFeaturesOutput(featureSql, queryName)
        ).build();
      }
    }
  }

    private JSONObject getRegionStatsOutput(String featureSql, int basesPerBin, Long start, Long inclusiveEnd, String queryName) {
        return new SQLRunner(_appDs, featureSql, queryName).executeQuery(rs -> {

      // for the purpose of binning, set 'end' to 1 beyond the inclusive end to make 'end' exclusive (eases calculations)
      long end = inclusiveEnd + 1;

      // define the bin array size and initialize
      long rangeLength = end - start; // number of values
      int binCount = Long.valueOf((rangeLength / basesPerBin) + (rangeLength % basesPerBin == 0 ? 0 : 1)).intValue();
      int[] bins = new int[binCount];
      Arrays.fill(bins, 0);

      // getBinIndex assigns a location to a bin
      Function<Long,Long> getBinIndex = location ->
        location < start ? 0 :
          location >= end ? binCount - 1 :
            (location - start) / basesPerBin;

      while (rs.next()) {
        // basic feature json has only start and end properties
        JSONObject featureJson = getFeatureRangeJson(rs);
        Long startBinIndex = getBinIndex.apply(featureJson.getLong("start"));
        Long endBinIndex = getBinIndex.apply(featureJson.getLong("end"));

        // add one to each bin this feature crosses
        for (Long i = startBinIndex; i <= endBinIndex; i++) {
          bins[i.intValue()]++;
        }
      }

      // stats JSON should look like this: {
      //   bins: int[],
      //   stats: {
      //     basesPerBin: int,
      //     max: int
      //   }
      // }
      return new JSONObject()
        .put("bins", bins)
        .put("stats", new JSONObject()
          .put("basesPerBin", basesPerBin)
          .put("max", Arrays.stream(bins).max().orElse(0))
        );
      });
  }

  private static class Subfeature {
    public String topParentId;
    public String parentId;
    public String featureId;
    public JSONObject properties = new JSONObject();
    public List<TwoTuple<Long,Long>> tranges = new ArrayList<>();
    public List<Subfeature> children = new ArrayList<>();

    public List<JSONObject> getSubfeatureObjects() {

      JSONObject thisSubfeature = new JSONObject(properties, JSONObject.getNames(properties))
          .put("subfeatures", getChildrenJson(children));

      if (!children.isEmpty() && !tranges.isEmpty()) {
        throw new WdkRuntimeException("Subfeature '" + featureId +
            "', child of '" + parentId + "' has both child subfeatures and tblocks.");
      }

      List<JSONObject> subfeatureObjects = new ArrayList<>();
      if (tranges.isEmpty()) {
        // no tranges; need to add single object for this subfeature (may contain children)
        subfeatureObjects.add(thisSubfeature);
      }

      // add copies for tblocks
      for (int i = 0; i < tranges.size(); i++) {
        // make a copy for each with modified start and end values, and suffixed names
        JSONObject tmp = new JSONObject(properties, JSONObject.getNames(properties))
          .put("feature_id", featureId + "_" + i)
          .put("start", tranges.get(i).getFirst())
          .put("end", tranges.get(i).getSecond());
        if (tmp.has("name")) {
          tmp.put("name", tmp.getString("name") + "_" + i);
        }
        subfeatureObjects.add(tmp);
      }

      return subfeatureObjects;
    }
  }

    private StreamingOutput getFeaturesOutput(String featureSql, String queryName) {
    return outputStream -> {
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
      writer.write("{\"features\":[");
      new SQLRunner(_appDs, featureSql, queryName).executeQuery(featureRs -> {
        try {
          boolean featureHasAtts = hasColumn(featureRs, "ATTS");
          boolean featureHasUniqueId = hasColumn(featureRs, "FEATURE_ID");
          boolean firstRow = true;
          while (featureRs.next()) {
            JSONObject featureJson = getFeatureRangeJson(featureRs);
            appendColumnValues(featureJson, featureRs);
            if (featureHasAtts) appendAttributes(featureJson, featureRs);
            String uniqueId = null;
            if (featureHasUniqueId) uniqueId = featureRs.getString("FEATURE_ID");
            featureJson.put("uniqueID", uniqueId);
            featureJson.put("subfeatures", new JSONArray());
            if (firstRow) firstRow = false; else writer.write(",");
            writer.write(featureJson.toString());
          }
          return null;
        }
        catch (IOException e) {
          throw new SQLRunnerException(e);
        }
      });
      writer.write("]}");
      writer.flush();
    };
  }

    private StreamingOutput getFeaturesOutput(String featureSql, String bulkSubfeatureSql, String queryName) {

    // wrap feature SQL with an order by
    String sortedFeatureSql = "select * from ( " + featureSql + NL + " ) order by feature_id asc";

    // build subfeature SQL that orders all subfeatures by their top-level parent's feature_id
    String wrappedSubfeatureSql = "( select * from ( " + bulkSubfeatureSql + NL +" ) )";
    String sortedSubfeatureSql =
        " select * from (" +
        "   select a.*," +
        "     (case when b.parent_id is null then a.parent_id else b.parent_id end) as top_parent_id" +
        "   from " + wrappedSubfeatureSql + " a" +
        "   left join " + wrappedSubfeatureSql + " b on a.parent_id = b.feature_id" +
        " )" +
        " order by top_parent_id asc";

    LOG.debug("Subfeature SQL!!!\n" + bulkSubfeatureSql);
    LOG.debug("Sorted subfeature SQL!!!\n" + sortedSubfeatureSql);

    return outputStream -> {
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
      writer.write("{\"features\":[");
      try (
        Connection con1 = _appDs.getConnection();
        Connection con2 = _appDs.getConnection();
      ) {
              new SQLRunner(con1, sortedFeatureSql, queryName).executeQuery(featureRs -> {
                      return new SQLRunner(con2, sortedSubfeatureSql, queryName + "_bulk_sub_features").executeQuery(subfeatureRs -> {
            try {
              boolean featureHasAtts = hasColumn(featureRs, "ATTS");
              boolean subfeatureHasAtts = hasColumn(subfeatureRs, "ATTS");
              boolean subfeatureHasTStarts = hasColumn(subfeatureRs, "TSTARTS");
              List<Subfeature> currentSubfeatures = new ArrayList<>();
              Subfeature nextSubfeature = null;
              boolean firstRow = true;
              // TODO: figure out if this is a sufficient collator!  May have different sorting than Oracle's default
              //    Need this one? https://docs.oracle.com/database/121/JVGDK/oracle/i18n/text/OraCollator.html
              Collator collator = Collator.getInstance(Locale.US);
              while (featureRs.next()) {

                // build feature JSON object
                JSONObject featureJson = getFeatureRangeJson(featureRs);
                appendColumnValues(featureJson, featureRs);
                if (featureHasAtts) appendAttributes(featureJson, featureRs);
                String featureId = featureRs.getString("FEATURE_ID");
                featureJson.put("uniqueID", featureId);

                // reads subfeatures until the top_parent_id does not match the current feature_id
                nextSubfeature = populateCurrentSubfeatures(featureId, currentSubfeatures,
                    nextSubfeature, subfeatureRs, subfeatureHasAtts, subfeatureHasTStarts, collator);

                // add any matching subfeatures found to the feature JSON
                featureJson.put("subfeatures", combineSubfeatures(featureId, currentSubfeatures));

                // write this feature to the output stream
                if (firstRow) firstRow = false; else writer.write(",");
                writer.write(featureJson.toString());
              }
              return null;
            }
            catch (IOException e) {
              throw new SQLRunnerException(e);
            }
          });
        });
      }
      catch (SQLException e) {
        throw new RuntimeException("Unable to connect to database", e);
      }
      writer.write("]}");
      writer.flush();
    };
  }

  private static JSONArray combineSubfeatures(String featureId, List<Subfeature> subfeatures) {
    // build map of 2nd tier subfeatures (direct children of the feature)
    Map<String, Subfeature> secondTierSubfeatures = new HashMap<>();
    for (Subfeature subfeature : subfeatures) {
      if (subfeature.parentId.equals(featureId)) {
        secondTierSubfeatures.put(subfeature.featureId, subfeature);
      }
    }
    // assign feature's grandchildren to their parents, logging if parents cannot be found
    for (Subfeature subfeature : subfeatures) {
      if (!subfeature.parentId.equals(featureId)) {
        Subfeature secondTierSubfeature = secondTierSubfeatures.get(subfeature.parentId);
        if (secondTierSubfeature == null) {
          LOG.warn("Subfeature SQL produced 3rd tier subfeature that did not belong to any 2nd tier subfeatures of feature with ID " + featureId);
        }
        else {
          secondTierSubfeature.children.add(subfeature);
        }
      }
    }
    return getChildrenJson(secondTierSubfeatures.values());
  }

  private static JSONArray getChildrenJson(Collection<Subfeature> children) {
    List<JSONObject> subfeatureList = new ArrayList<>();
    for (Subfeature child : children) {
      subfeatureList.addAll(child.getSubfeatureObjects());
    }
    return new JSONArray(subfeatureList);
  }

  private static Subfeature populateCurrentSubfeatures(String featureId, List<Subfeature> currentSubfeatures,
      Subfeature nextSubfeature, ResultSet subfeatureRs, boolean subfeatureHasAtts, boolean subfeatureHasTStarts, Collator collator) throws SQLException {
    currentSubfeatures.clear();
    // If this is the first time this method is called; need to load a "base" subfeature
    if (nextSubfeature == null) {
      if (subfeatureRs.next()) {
        nextSubfeature = readSubfeature(subfeatureRs, subfeatureHasAtts, subfeatureHasTStarts);
      }
      else {
        // no more subfeatures in the result
        return null;
      }
    }

    // The feature and subfeature result sets are sorted by feature_id ascending;
    // some subfeaures included in result may not match features in the feature result
    // if the subfeature's top parent is "lower" than the feature_id, then
    //    need to throw away subfeatures until we find one greater than or equal to
    while (collator.compare(nextSubfeature.topParentId, featureId) < 0) {
      // subfeature's top parent ID is less than current feature ID; load next
      if (subfeatureRs.next()) {
        nextSubfeature = readSubfeature(subfeatureRs, subfeatureHasAtts, subfeatureHasTStarts);
      }
      else {
        // no more subfeatures in the result; the rest of the features do not have subfeatures
        return null;
      }
    }

    // next subfeature's top parent ID is equal to or greater than current feature ID
    while (collator.compare(nextSubfeature.topParentId, featureId) == 0) {
      // this subfeature matches current feature; add to list and load next
      currentSubfeatures.add(nextSubfeature);
      if (subfeatureRs.next()) {
        nextSubfeature = readSubfeature(subfeatureRs, subfeatureHasAtts, subfeatureHasTStarts);
      }
      else {
        // no more subfeatures in the result; the rest of the features do not have subfeatures
        return null;
      }
    }

    // current subfeature does not belong to current feature; no more subfeatures for this feature
    return nextSubfeature;
  }


  private static Subfeature readSubfeature(ResultSet subfeatureRs, boolean hasAtts, boolean hasTStarts) throws SQLException {
    Subfeature subfeature = new Subfeature();
    subfeature.topParentId = subfeatureRs.getString("top_parent_id");
    subfeature.parentId = subfeatureRs.getString("parent_id");
    subfeature.featureId = subfeatureRs.getString("feature_id");
    subfeature.properties = getFeatureRangeJson(subfeatureRs);
    appendColumnValues(subfeature.properties, subfeatureRs);
    if (hasAtts) {
      appendAttributes(subfeature.properties, subfeatureRs);
    }
    if (hasTStarts) {
      String[] tstarts = subfeatureRs.getString("TSTARTS").split(",");
      String[] blocksizes = subfeatureRs.getString("BLOCKSIZES").split(",");
      for (int i = 0; i < tstarts.length; i++) {
        long tstart = Long.valueOf(tstarts[i]) - 1;
        long tend = tstart + Long.valueOf(blocksizes[i]);
        subfeature.tranges.add(new TwoTuple<>(tstart, tend));
      }
    }
    return subfeature;
  }

  // NOTE: locations have shown to be larger than MAX_INT so use longs here
  private static JSONObject getFeatureRangeJson(ResultSet featureRs) throws SQLException {
    long startm = featureRs.getLong("STARTM");
    long featureStart = startm - 1;
    long featureEnd = featureRs.getLong("END");
    return new JSONObject()
      .put("start", featureStart)
      .put("end", featureEnd);
  }

  private static List<String> INTERNAL_COLUMN_NAMES = Arrays.asList(new String[]{
    "parent_id",
    "top_parent_id",
    "atts",
    "tstarts",
    "blocksizes"
  });

  private static void appendColumnValues(JSONObject myFeature, ResultSet featureRs) throws SQLException {
    ResultSetMetaData rsmd = featureRs.getMetaData();
    int columnCount = rsmd.getColumnCount();
    for (int i = 1; i <= columnCount; i++) {
      String colLabel = rsmd.getColumnLabel(i).toLowerCase();
      if (!INTERNAL_COLUMN_NAMES.contains(colLabel)) {
        myFeature.put(colLabel, featureRs.getString(i));
      }
    }
  }

  private Optional<String> getBulkSubfeatureSql(String feature,
      String seqId, String featureSql, Map<String, String> qp, Category category) {
    String bulksubfeature = feature + ":bulksubfeatures";
    String subfeatureSql = JBrowseQueries.getQueryMap(_projectId, category).get(bulksubfeature);
    if (subfeatureSql == null) {
      return Optional.empty();
    }

    String queryName = feature + "_range";

    return findFeatureRange(featureSql, queryName).map(featureRange ->
      replaceSqlMacros(subfeatureSql,
        featureRange.getBegin().toString(),
        featureRange.getEnd().toString(), seqId, qp));
  }

  private static void appendAttributes(JSONObject json, ResultSet rs) throws SQLException {
    String attrsStr = rs.getString("ATTS");
    if (rs.wasNull()) return;
    String attrs[] = attrsStr.split(";;");
    for (int i = 0; i < attrs.length; i++) {
      String attr[] = attrs[i].split("=");
      if (attr.length > 1) {
        json.put(attr[0].toLowerCase(), attr[1]);
      }
    }
  }

    private Optional<Range<Integer>> findFeatureRange(String featureSql, String queryName) {
    // find range of selected features
    String rangeSql = "select min(startm) as min_start, max(\"end\") as max_end from ( " + featureSql + " )";
    return new SQLRunner(_appDs, rangeSql, queryName).executeQuery(rs -> {
      if (!rs.next()) return Optional.empty();
      int minStart = rs.getInt("min_start");
      if (rs.wasNull()) return Optional.empty();
      int maxEnd = rs.getInt("max_end");
      if (rs.wasNull()) return Optional.empty();
      return Optional.of(new Range<Integer>(minStart - 1, maxEnd));
    });
  }

  private String getFeatureSql(String refseqName, String feature,
      Long start, Long end, Map<String, String> qp, boolean isReferenceFeature,
      String sequenceTableName, Category category, String seqId) {
    if (isReferenceFeature) {
      Long length = end - start;
      start = (start < 0) ? 0 : start;
      return "select " +
          "substr(sequence, " + start.toString() + ", " + length.toString() + ") as seq, " +
          start.toString() + " as startm, " +
          end.toString() + " as \"end\", '" +
          refseqName + "' as feature_id " +
          "from " + sequenceTableName +
          " where source_id = '" + refseqName + "'";
    }
    else {
      String baseSql = JBrowseQueries.getQueryMap(_projectId, category).get(feature);
      if (baseSql == null) {
        throw new NotFoundException("Cannot find feature query for projectId=" +
          _projectId + ", category=" + category + ", feature=" + feature);
      }
      return replaceSqlMacros(baseSql, start.toString(), end.toString(), seqId, qp);
    }
  }

    private Optional<String> getSequenceId(String idColName, String attrsTableName, String refseqName, String queryName) {
    String seqIdSql = "select " + idColName + " from " + attrsTableName + " where source_id = '" + refseqName + "'";
    return new SQLRunner(_appDs, seqIdSql, queryName)
        .executeQuery(rs -> rs.next() ? Optional.ofNullable(rs.getString(1)) : Optional.empty());
  }

  private static String replaceSqlMacros(String sql, String start, String end, String seqId, Map<String, String> qp) {
    sql = sql.replaceAll("\\$base_start", start);
    sql = sql.replaceAll("\\$rend", end);
    sql = sql.replaceAll("\\$dlm",";;");
    sql = sql.replaceAll("\\$srcfeature_id", seqId);

    // skip query params we know are used for "control" purposes (not SQL macros)
    List<String> skipMe = new ArrayList<>() {{
      add("feature");
      add("start");
      add("end");
      add("seqType");
    }};

    for (Map.Entry<String, String> entry : qp.entrySet()) {
      if (!skipMe.contains(entry.getKey())) {
        sql = sql.replaceAll("\\$\\$" + entry.getKey() + "\\$\\$", entry.getValue());
      }
    }

    return sql;
  }

  private static boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
    ResultSetMetaData rsmd = rs.getMetaData();
    int columns = rsmd.getColumnCount();
    for (int x = 1; x <= columns; x++) {
      if (columnName.equals(rsmd.getColumnLabel(x))) {
        return true;
      }
    }
    return false;
  }

}
