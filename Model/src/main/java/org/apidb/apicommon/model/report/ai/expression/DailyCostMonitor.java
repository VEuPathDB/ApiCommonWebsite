package org.apidb.apicommon.model.report.ai.expression;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.cache.disk.DirectoryLock;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.json.JSONException;
import org.json.JSONObject;

import com.openai.models.CompletionUsage;

public class DailyCostMonitor {

  private static final Logger LOG = Logger.getLogger(DailyCostMonitor.class);

  // daily cost monitoring locations
  private static final String DAILY_COST_ACCUMULATION_FILE_DIR = "dailyCost";
  private static final String DAILY_COST_ACCUMULATION_FILE = "daily_cost_accumulation.txt";

  // model prop keys
  private static final String MAX_DAILY_DOLLAR_COST_PROP_NAME = "OPENAI_MAX_DAILY_AI_EXPRESSION_DOLLAR_COST";
  private static final String DOLLAR_COST_PER_1M_INPUT_TOKENS_PROP_NAME = "OPENAI_DOLLAR_COST_PER_1M_AI_INPUT_TOKENS";
  private static final String DOLLAR_COST_PER_1M_OUTPUT_TOKENS_PROP_NAME = "OPENAI_DOLLAR_COST_PER_1M_AI_OUTPUT_TOKENS";

  // lock characteristics
  private static final long DEFAULT_TIMEOUT_MILLIS = 1000;
  private static final long DEFAULT_POLL_FREQUENCY_MILLIS = 50;

  // json props
  private static final String JSON_DATE_PROP = "currentDate";
  private static final String JSON_COST_PROP = "accumulatedCost";

  // completion usage object representing 0 cost
  private static final CompletionUsage EMPTY_COST = CompletionUsage.builder()
      .promptTokens(0)
      .completionTokens(0)
      .totalTokens(0)
      .build();

  private final Path _costMonitoringDir;
  private final Path _costMonitoringFile;

  private final double _maxDailyDollarCost;
  private final double _costPerInputToken;
  private final double _costPerOutputToken;

  public DailyCostMonitor(WdkModel wdkModel) throws WdkModelException {
    _costMonitoringDir = AiExpressionCache.getAiExpressionCacheParentDir(wdkModel).resolve(DAILY_COST_ACCUMULATION_FILE_DIR).toAbsolutePath();
    LOG.debug("Attempting creation of open perms cost monitoring dir: " + _costMonitoringDir);
    Utilities.ensureCreation(Files::createDirectory, _costMonitoringDir);
    if (!Files.exists(_costMonitoringDir) || !Files.isReadable(_costMonitoringDir) || !Files.isWritable(_costMonitoringDir)) {
      throw new WdkModelException("Directory " + _costMonitoringDir + " does not exist or is not readable/writeable by this user.");
    }

    _costMonitoringFile = _costMonitoringDir.resolve(DAILY_COST_ACCUMULATION_FILE);

    _maxDailyDollarCost = getNumberProp(wdkModel, MAX_DAILY_DOLLAR_COST_PROP_NAME);
    _costPerInputToken = getNumberProp(wdkModel, DOLLAR_COST_PER_1M_INPUT_TOKENS_PROP_NAME) / 1000000;
    _costPerOutputToken = getNumberProp(wdkModel, DOLLAR_COST_PER_1M_OUTPUT_TOKENS_PROP_NAME) / 1000000;
  }

  private double getNumberProp(WdkModel wdkModel, String propName) throws WdkModelException {
    try {
      return Double.parseDouble(Optional
          .ofNullable(wdkModel.getProperties().get(propName))
          .orElseThrow(() -> new WdkModelException("WDK property '" + propName + "' has not been set.")));
    }
    catch (NumberFormatException e) {
      throw new WdkModelException("WDK property '" + propName + "' must be a number.");
    }
  }

  public boolean isCostExceeded() {
    return updateAndGetCost(EMPTY_COST) > _maxDailyDollarCost;
  }

  public void updateCost(Optional<CompletionUsage> usage) {
    updateAndGetCost(usage.orElse(EMPTY_COST));
  }

  public double updateAndGetCost(CompletionUsage usageCost) {
    try (DirectoryLock lock = new DirectoryLock(_costMonitoringDir, DEFAULT_TIMEOUT_MILLIS, DEFAULT_POLL_FREQUENCY_MILLIS)) {

      // read current values from file
      JSONObject previousJson = readAccumulatedCostFile();
      String previousDate = previousJson.getString(JSON_DATE_PROP);
      double previousCost = previousJson.getDouble(JSON_COST_PROP);

      // calculate cost of the current usage
      double additionalCost =
          (usageCost.promptTokens() * _costPerInputToken) +
          (usageCost.completionTokens() * _costPerOutputToken);

      // reset cost to zero if date has rolled over to the next day
      String newDate = getCurrentDateString();
      if (!newDate.equals(previousDate)) {
        // reset cost
        previousCost = 0;
      }

      double newCost = previousCost + additionalCost;

      // only write file if necessary
      if (!newDate.equals(previousDate) || newCost != previousCost) {

        String fileJson = new JSONObject()
            .put(JSON_DATE_PROP, newDate)
            .put(JSON_COST_PROP, newCost)
            .toString();
        LOG.info("Updating daily cost file: " + fileJson);
        Utilities.ensureCreation(Files::createFile, _costMonitoringFile);
        try (Writer out = new FileWriter(_costMonitoringFile.toFile())) {
          out.write(fileJson);
        }
      }

      return newCost;
    }
    catch (Exception e) {
      throw new WdkRuntimeException("Unable to update AI expression cost", e);
    }
  }

  private static String getCurrentDateString() {
    return FormatUtil.STANDARD_DATE_FORMAT.format(LocalDate.now());
  }

  private JSONObject readAccumulatedCostFile() {
    if (!_costMonitoringFile.toFile().exists()) {
      return new JSONObject()
          .put(JSON_DATE_PROP, getCurrentDateString())
          .put(JSON_COST_PROP, 0);
    }
    try (Reader in = new FileReader(_costMonitoringFile.toFile())) {
      return new JSONObject(IoUtil.readAllChars(in));
    }
    catch (IOException | JSONException e) {
      throw new WdkRuntimeException("Unable to read or parse AI expression cost file: " + _costMonitoringFile, e);
    }
  }

}
