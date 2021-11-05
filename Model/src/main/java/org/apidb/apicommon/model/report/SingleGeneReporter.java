package org.apidb.apicommon.model.report;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.answer.stream.RecordStream;
import org.gusdb.wdk.model.answer.stream.RecordStreamFactory;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.record.TableField;
import org.gusdb.wdk.model.record.attribute.AttributeField;
import org.gusdb.wdk.model.report.AbstractReporter;
import org.gusdb.wdk.model.report.Reporter;
import org.gusdb.wdk.model.report.ReporterConfigException;
import org.gusdb.wdk.model.report.ReporterConfigException.ErrorType;
import org.json.JSONException;
import org.json.JSONObject;
import org.apidb.apicommon.model.TranscriptUtil;
import org.apidb.apicommon.model.report.singlegeneformats.ApolloGoTermFormat;

public class SingleGeneReporter extends AbstractReporter {

  public interface Format {
    List<String> getRequiredAttributeNames();
    List<String> getRequiredTableNames();
    JSONObject writeJson(RecordInstance recordInstance) throws WdkModelException, WdkUserException;
  }

  private static enum FormatType {

    APOLLO_GO_TERM(() -> new ApolloGoTermFormat());

    private Supplier<? extends Format> _formatProvider;

    private FormatType(Supplier<? extends Format> formatProvider) {
      _formatProvider = formatProvider;
    }

    public Format getFormat() {
      return _formatProvider.get();
    }
  }

  private Format _format;

  public SingleGeneReporter(AnswerValue answerValue) {
    super(answerValue);
  }

  @Override
  public Reporter configure(JSONObject config) throws ReporterConfigException, WdkModelException {
    try {
      int resultSize = getResultSize();
      if (resultSize != 1) {
        throw new ReporterConfigException("This reporter can only be used on " +
            "results of size 1 (this result has size " + resultSize, ErrorType.DATA_VALIDATION);
      }
      String formatStr = config.getString("format");
      _format = FormatType.valueOf(formatStr.toUpperCase()).getFormat();
    }
    catch (JSONException e) {
      throw new ReporterConfigException(e.getMessage());
    }
    catch (IllegalArgumentException e) {
      throw new ReporterConfigException(e.getMessage(), ErrorType.DATA_VALIDATION);
    }
    return this;
  }

  @Override
  protected void write(OutputStream out) throws IOException, WdkModelException {
    RecordClass geneRecordClass = TranscriptUtil.getGeneRecordClass(_wdkModel);
    Map<String, AttributeField> attributeFields = geneRecordClass.getAttributeFieldMap();
    Map<String, TableField> tableFields = geneRecordClass.getTableFieldMap();
    List<AttributeField> attributes = _format.getRequiredAttributeNames().stream()
        .map(name -> attributeFields.get(name)).collect(Collectors.toList());
    List<TableField> tables = _format.getRequiredTableNames().stream()
        .map(name -> tableFields.get(name)).collect(Collectors.toList());
    try (RecordStream recordStream = RecordStreamFactory.getRecordStream(_baseAnswer, attributes, tables)) {
      RecordInstance singleRecord = recordStream.iterator().next();
      JSONObject formattedResult = _format.writeJson(singleRecord);
      out.write(formattedResult.toString().getBytes());
      out.flush();
    }
    catch (WdkUserException e) {
      throw new WdkModelException(e);
    }
  }

}
