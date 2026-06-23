package org.gusdb.wdk.model.report.reporter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.answer.stream.RecordStream;
import org.gusdb.wdk.model.answer.stream.RecordStreamFactory;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.record.TableField;
import org.gusdb.wdk.model.record.TableValue;
import org.gusdb.wdk.model.record.TableValueRow;
import org.gusdb.wdk.model.record.attribute.AttributeField;
import org.gusdb.wdk.model.record.attribute.AttributeValue;
import org.gusdb.wdk.model.report.AbstractReporter;
import org.gusdb.wdk.model.report.PropertiesProvider;
import org.gusdb.wdk.model.report.Reporter;
import org.gusdb.wdk.model.report.ReporterConfigException;
import org.gusdb.wdk.model.report.util.TableCache;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

/**
 * @author Steve
 *
 */
public class Jbrowse2ConfigReporter extends AbstractReporter {

  private static final Logger LOG = Logger.getLogger(Jbrowse2ConfigReporter.class);

  private TableCache _tableCache;

  @Override
  public Jbrowse2ConfigReporter setProperties(PropertiesProvider reporterRef) throws WdkModelException {
    super.setProperties(reporterRef);
    String cacheTableName = TableCache.getCacheTableName(_properties);
    if (cacheTableName != null) {
      _tableCache = new TableCache(getQuestion().getRecordClass(), _wdkModel.getAppDb(), cacheTableName);
    }
    return this;
  }

  @Override
  public Reporter configure(JSONObject config) throws ReporterConfigException, WdkModelException {
    return null;
  }

  @Override
  public String getHttpContentType() {
    return "application/json";
  }

  @Override
  public String getDownloadFileName() {
    return getQuestion().getName() + "_detail.json";
  }

  @Override
  public void write(OutputStream out) throws WdkModelException {

    Map<String, AttributeField> attrFieldMap = getQuestion().getAttributeFieldMap();
    if (!attrFieldMap.containsKey("short_display_name")) throw new WdkModelException();
    AttributeField af = attrFieldMap.get("short_display_name");
    Set<AttributeField> afs = new HashSet<>();
    afs.add(af);

    Map<String, TableField> tblFieldMap = getQuestion().;
    if (!tblFieldMap.containsKey("short_display_name")) throw new WdkModelException();
    TableField tf = tblFieldMap.get("short_display_name");
    Set<AttributeField> afs = new HashSet<>();
    afs.add(af);

    //   TableField udDependenciesTable = (TableField) fieldMap.get("UdDependencies");

    try (RecordStream records = RecordStreamFactory.getRecordStream(_baseAnswer, afs, tables)) {
      OutputStreamWriter streamWriter = new OutputStreamWriter(out);
      JSONWriter writer = new JSONWriter(streamWriter);

      AnswerValue av = _baseAnswer;
      writer.object().key("response").object().key("recordset").object().key("id").value(
              av.getChecksum()).key("count").value(this.getResultSize()).key("type").value(
              av.getAnswerSpec().getQuestion().getRecordClass().getDisplayName()).key("records").array();

      if (_tableCache != null) {
        _tableCache.open();
      }

      // get page based answers with a maximum size (defined in PageAnswerIterator)
      int recordCount = 0;
      for (RecordInstance record : records) {

        // get VDI ID
        String datasetId = record.getPrimaryKey().getValues().get("dataset_id");
        String suffix = "EDAUD_";
        if (!datasetId.startsWith(suffix)) throw new WdkUserException();
        String vdiId = datasetId.substring(suffix.length());

        // get display name
        String shortDisplayName = record.getAttributeValue("short_display_name").getValue();

        // get organism name for files
        TableValue tv = record.getTableValue("UdDependencies");
        if (tv.getNumRows() != 1) throw new WdkUserException();
        TableValueRow tvr = tv.iterator().next();
        if (!tvr.containsKey("identifier")) throw new WdkUserException();
        String orgNameForFiles = tvr.get("identifier").getValue();

        // count the records processed so far
        recordCount++;
        writer.endObject();
        streamWriter.flush();
      }

      writer.endArray() // records
              .endObject().endObject().endObject();
      streamWriter.flush();
      LOG.info("Totally " + recordCount + " records dumped");
    } catch (WdkUserException | JSONException | SQLException | IOException e) {
      throw new WdkModelException("Unable to write JSON report", e);
    } finally {
      if (_tableCache != null) {
        _tableCache.close();
      }
    }
  }

  /*
    {
      "assemblyNames": [
        "VDI_ORGANISM_FOR_FILES"
      ],
      "trackId": "UD_VDI_ID",
      "name": "SHORT_DISPLAY_NAME",
      "displays": [
        {
          "displayId": "wiggle_ApiCommonModel::Model::JBrowseTrackConfig::MultiBigWigTrackConfig::XY=HASH(0x2249320)",
          "maxScore": 1000,
          "minScore": 1,
          "defaultRendering": "multirowxy",
          "type": "MultiLinearWiggleDisplay",
          "scaleType": "log"
        }
      ],
      "adapter": {
        "subadapters": [
          {
            "color": "grey",
            "name": "Unsporulated oocyst (non-unique)",
            "type": "BigWigAdapter",
            "bigWigLocation": {
              "locationType": "UriLocation",
              "uri": "ToxoDB/build-68/EtenellaHoughton2021/bigwig/etenHoughton2021_Reid_RNASeq_ebi_rnaSeq_RSRC/1_Unsporulated_oocyst/non_
unique_resultsCombinedReps_unlogged.bw"
            }
          }
*/


  private static void formatAttributes(RecordInstance record, Set<AttributeField> attributes, JSONWriter writer)
          throws WdkModelException, WdkUserException {
    if (!attributes.isEmpty()) {
      writer.key("fields").array();
      for (AttributeField field : attributes) {
        AttributeValue value = record.getAttributeValue(field.getName());
        writer.object().key("name").value(field.getName()).key("value").value(value.getValue()).endObject();
      }
      writer.endArray();
    }
  }

  /*
  - dataset id
  -
   */
  private Set<AttributeField> getAttributes() {
    return null;
  }
  private Set<TableField> getTables() {
    return null;
  }

}


