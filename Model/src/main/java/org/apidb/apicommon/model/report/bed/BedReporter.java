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

public abstract class BedReporter extends AbstractReporter {

  private static Logger LOG = Logger.getLogger(BedReporter.class);

  private BedFeatureProvider _featureProvider;
  private Collection<AttributeField> _requiredAttributes = Collections.emptyList();
  private Collection<TableField> _requiredTables = Collections.emptyList();

  protected Reporter configure(BedFeatureProvider featureProvider) {

    // validate required record class matches the one in the answer value
    RecordClass recordClass = getQuestion().getRecordClass();
    String recordClassFullName = recordClass.getFullName();
    String requiredRecordClassFullName = featureProvider.getRequiredRecordClassFullName();
    if (!recordClassFullName.equals(requiredRecordClassFullName)){
      throw new IllegalStateException("This configuration requires a record class " + requiredRecordClassFullName + ", found: " + recordClassFullName);
    }

    // fetch and validate requested fields
    _requiredAttributes = getFieldsByName(recordClass, featureProvider.getRequiredAttributeNames(), (rc,name) -> rc.getAttributeField(name));
    _requiredTables = getFieldsByName(recordClass, featureProvider.getRequiredTableNames(), (rc,name) -> Optional.ofNullable(rc.getTableFieldMap().get(name)));

    // save off feature provider to process records
    _featureProvider = featureProvider;

    return this;
  }

  @Override
  public String getHttpContentType() {
    return "text/x-bed";
  }

  @Override
  public String getDownloadFileName() {
    return getQuestion().getName() + ".bed";
  }

  @Override
  public void write(OutputStream out) throws WdkModelException {
    if (_featureProvider == null) {
      throw new IllegalStateException("configure(BedFeatureProvider) method must be called by subclass during configuration");
    }
    try (RecordStream records = RecordStreamFactory.getRecordStream(_baseAnswer, _requiredAttributes, _requiredTables);
         BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out))) {
      int recordCount = 0;
      for (RecordInstance record : records) {
        recordCount++;
        for (List<String> line: _featureProvider.getRecordAsBedFields(record)){
          writer.write(String.join("\t", line));
          writer.newLine();
        }
      }
      writer.flush();
      LOG.info("Wrote " + recordCount + " records");
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
