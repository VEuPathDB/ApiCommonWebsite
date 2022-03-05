package org.apidb.apicommon.model.report;

import static org.apidb.apicommon.model.report.singlegeneformats.Formats.FORMAT_MAP;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apidb.apicommon.model.TranscriptUtil;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
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

public class SingleGeneReporter extends AbstractReporter {

  public interface Format {
    //String getFormatName();
    List<String> getRequiredAttributeNames();
    List<String> getRequiredTableNames();
    JSONObject writeJson(RecordInstance recordInstance) throws WdkModelException, WdkUserException;
  }

  private Format _format;

  @Override
  public Reporter configure(JSONObject config) throws ReporterConfigException, WdkModelException {
    try {
      int resultSize = getResultSize();
      if (resultSize != 1) {
        throw new ReporterConfigException("This reporter can only be used on " +
            "results of size 1 (this result has size " + resultSize, ErrorType.DATA_VALIDATION);
      }
      String formatStr = config.getString("format");
      Supplier<Format> supplier = FORMAT_MAP.get(formatStr);
      if (supplier == null) {
        throw new ReporterConfigException("Unrecognized format: " + formatStr +
            "; must be one of: " + FormatUtil.join(FORMAT_MAP.keySet(), ", "), ErrorType.DATA_VALIDATION);
      }
      _format = supplier.get();
    }
    catch (JSONException e) {
      throw new ReporterConfigException(e.getMessage());
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

  /** TODO: Fix to avoid need for explicit format map; does not work :(
  private static Map<String, Supplier<Format>> generateFormatMap() {
    Map<String,Supplier<Format>> map = new ConcurrentHashMap<>();
    List<Class<Format>> formatClasses = ClassFinder.getClassesBySubtype(Format.class, "org.apidb.apicommon.model.report.singlegeneformats");
    for (Class<Format> formatClass : formatClasses) {
      String ignoreMessagePrefix = "Ignoring Format class " + formatClass.getName() + ": ";
      Supplier<Format> supplier = () -> {
        try {
          return formatClass.getConstructor().newInstance();
        }
        catch (NoSuchMethodException | SecurityException | InstantiationException |
               IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
          return null;
        }
      };
      Format test = supplier.get();
      if (test == null) {
        LOG.warn(ignoreMessagePrefix + "No no-arg constructor present.");
      }
      else {
        // instantiation succeeded
        String formatName = test.getFormatName();
        Objects.nonNull(formatName);
        if (map.containsKey(formatName)) {
          LOG.warn(ignoreMessagePrefix + "Format name '" + formatName +
              "' already taken by " + map.get(formatName).get().getClass().getName());
        }
        else {
          map.put(formatName, supplier);
        }
      }
    }
    return map;
  }*/
}
