package org.apidb.apicommon.model.report.bed.feature;

import java.util.List;
import java.util.Optional;

import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.record.RecordInstance;

public interface BedFeatureProvider {

  enum OffsetSign {
    plus,
    minus;
  }

  enum Anchor {
    Start,
    End
  }

  /**
   * @return full name of required record class
   */
  String getRequiredRecordClassFullName();

  /**
   * @return names of required attribute fields (must exist on record class above)
   */
  String[] getRequiredAttributeNames();

  /**
   * @return names of required table fields (must exist on record class above)
   */
  String[] getRequiredTableNames();

  /**
   * spec: https://en.wikipedia.org/wiki/BED_(file_format)
   *
   * @param record record from which attributes should be extracted
   * @return return one or more lines
   * @throws WdkModelException if unable to read required data from record
   */
  List<List<String>> getRecordAsBedFields(RecordInstance record) throws WdkModelException;


  // convenience method to get the record's primary key's source_id column
  default String getSourceId(RecordInstance record) {
    return Optional.ofNullable(record.getPrimaryKey().getValues().get("source_id")).orElseThrow(() ->
        new RuntimeException("PK of RecordClass " + record.getRecordClass().getFullName() + " does not contain source_id column"));
  }

  // convenience method to read attribute string value w/o exceptions
  default String stringValue(RecordInstance record, String key){
    try {
      return record.getAttributeValue(key).toString();
    }
    catch (WdkModelException | WdkUserException e){
      throw new WdkRuntimeException(e);
    }
  }

}
