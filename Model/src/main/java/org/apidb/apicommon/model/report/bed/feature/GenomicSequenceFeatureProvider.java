package org.apidb.apicommon.model.report.bed.feature;

import java.util.List;

import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.record.RecordInstance;
import org.json.JSONObject;
import org.apidb.apicommon.model.report.bed.util.StrandDirection;
import org.apidb.apicommon.model.report.bed.util.RequestedDeflineFields;
import org.apidb.apicommon.model.report.bed.util.DeflineBuilder;
import org.apidb.apicommon.model.report.bed.util.BedLine;

public class GenomicSequenceFeatureProvider implements BedFeatureProvider {

  private static final String ATTR_FORMATTED_LENGTH = "formatted_length";
  private static final String ATTR_ORGANISM = "organism";

  private final RequestedDeflineFields _requestedDeflineFields;
  private final StrandDirection _strand;

  public GenomicSequenceFeatureProvider(JSONObject config) {
    _requestedDeflineFields = new RequestedDeflineFields(config);
    _strand = StrandDirection.valueOf(config.getString("strand"));
  }

  @Override
  public String getRequiredRecordClassFullName() {
    return "SequenceRecordClasses.SequenceRecordClass";
  }

  @Override
  public String[] getRequiredAttributeNames() {
    return new String[] {
        ATTR_FORMATTED_LENGTH,
        ATTR_ORGANISM
    };
  }

  @Override
  public String[] getRequiredTableNames() {
    return new String[0];
  }

  @Override
  public List<List<String>> getRecordAsBedFields(RecordInstance record) throws WdkModelException {
    String featureId = getSourceId(record);
    String chrom = featureId;
    Integer featureLength = Integer.valueOf(stringValue(record, ATTR_FORMATTED_LENGTH).replaceAll(",", ""));
    Integer segmentStart = 1;
    Integer segmentEnd = featureLength;

    String formattedId = String.format("%s (%s)", chrom, _strand.getSign());

    DeflineBuilder defline = new DeflineBuilder(formattedId);

    if(_requestedDeflineFields.contains("organism")){
      defline.appendRecordAttribute(record, ATTR_ORGANISM);
    }
    if(_requestedDeflineFields.contains("description")){
      defline.appendValue("genomic sequence");
    }
    if(_requestedDeflineFields.contains("position")){
      defline.appendPosition(chrom, segmentStart, segmentEnd, _strand);
    }
    if(_requestedDeflineFields.contains("ui_choice")){
      defline.appendGenomicFeatureUiChoice("Whole sequence", _strand);
    }
    if(_requestedDeflineFields.contains("segment_length")){
      defline.appendSegmentLength(segmentStart, segmentEnd);
    }
    return List.of(BedLine.bed6(chrom, segmentStart, segmentEnd, defline, _strand));
  }

}
