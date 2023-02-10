package org.apidb.apicommon.model.report.bed;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import org.apache.log4j.Logger;
import org.apidb.apicommon.model.report.bed.feature.BedFeatureProvider;
import org.gusdb.fgputil.functional.FunctionalInterfaces.SupplierWithException;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.gusdb.wdk.model.answer.stream.RecordStream;
import org.gusdb.wdk.model.answer.stream.RecordStreamFactory;
import org.gusdb.wdk.model.record.Field;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.record.TableField;
import org.gusdb.wdk.model.record.attribute.AttributeField;
import org.gusdb.wdk.model.report.AbstractReporter;
import org.gusdb.wdk.model.report.Reporter;
import org.gusdb.wdk.model.report.ReporterConfigException;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class BedReporter extends AbstractReporter {

  private static Logger LOG = Logger.getLogger(BedReporter.class);

  public static final String EMPTY_FEATURE_OUTPUT = "### The result is empty ###";

  private BedFeatureProvider _featureProvider;
  private Collection<AttributeField> _requiredAttributes = Collections.emptyList();
  private Collection<TableField> _requiredTables = Collections.emptyList();
  protected boolean _isDownload;

  protected Reporter configure(SupplierWithException<BedFeatureProvider> featureProvider, ContentDisposition contentDisposition) throws ReporterConfigException {
    try {
      // save off feature provider to process records
      _featureProvider = featureProvider.get();

      // validate required record class matches the one in the answer value
      RecordClass recordClass = getQuestion().getRecordClass();
      String recordClassFullName = recordClass.getFullName();
      String requiredRecordClassFullName = _featureProvider.getRequiredRecordClassFullName();
      if (!recordClassFullName.equals(requiredRecordClassFullName)){
        throw new IllegalStateException("This configuration requires a record class " + requiredRecordClassFullName + ", found: " + recordClassFullName);
      }

      // fetch and validate requested fields
      _requiredAttributes = getFieldsByName(recordClass, _featureProvider.getRequiredAttributeNames(), (rc,name) -> rc.getAttributeField(name));
      _requiredTables = getFieldsByName(recordClass, _featureProvider.getRequiredTableNames(), (rc,name) -> Optional.ofNullable(rc.getTableFieldMap().get(name)));

      // determine content disposition
      _isDownload = contentDisposition == ContentDisposition.ATTACHMENT;

      return this;
    }
    // catch common configuration parsing runtime exceptions and convert for 400s
    catch (JSONException | IllegalArgumentException e) {
      throw new ReporterConfigException(e.getMessage());
    }
    catch (Exception e) {
      throw new WdkRuntimeException(e);
    }
  }

  protected ContentDisposition getContentDisposition(JSONObject config) {
    String attachmentType = config.optString("attachmentType", "plain");
    return "plain".equals(attachmentType)
        ? ContentDisposition.INLINE
        : ContentDisposition.ATTACHMENT;
  }

  @Override
  public String getHttpContentType() {
    return "text/x-bed";
  }

  @Override
  public String getDownloadFileName() {
    // null filename will indicate inline contentDisposition
    return _isDownload ? getQuestion().getName() + ".bed" : null;
  }

  @Override
  public void write(OutputStream out) throws WdkModelException {
    if (_featureProvider == null) {
      throw new IllegalStateException("configure(BedFeatureProvider) method must be called by subclass during configuration");
    }
    try (RecordStream records = RecordStreamFactory.getRecordStream(_baseAnswer, _requiredAttributes, _requiredTables);
         BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out))) {
      int recordCount = 0;
      int featureCount = 0;
      for (RecordInstance record : records) {
        recordCount++;
        for (List<String> line: _featureProvider.getRecordAsBedFields(record)){
          featureCount++;
          writer.write(String.join("\t", line));
          writer.newLine();
        }
      }
      LOG.info("Wrote " + featureCount + " features for " + recordCount + " records");
      if (featureCount == 0) {
        LOG.info("Writing empty response value: " + EMPTY_FEATURE_OUTPUT);
        writer.write(EMPTY_FEATURE_OUTPUT);
      }
      writer.flush();
    }
    catch (IOException e) {
      throw new WdkModelException("Unable to complete delivery of bed report", e);
    }
  }

  private <T extends Field> Collection<T> getFieldsByName(RecordClass recordClass, String[] names, BiFunction<RecordClass,String,Optional<T>> getter) {
    List<T> attrs = new ArrayList<>();
    for (String name : names) {
      attrs.add(getter.apply(recordClass,name).orElseThrow(() -> new WdkRuntimeException(
          "Subclass " + getClass().getName() + " declared a required field '" +
          name + "' that does not exist on RecordClass " + recordClass.getFullName())));
    }
    return attrs;
  }
}
