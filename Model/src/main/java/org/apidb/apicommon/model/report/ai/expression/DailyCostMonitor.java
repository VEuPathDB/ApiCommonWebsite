package org.apidb.apicommon.model.report.ai.expression;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.cache.disk.DirectoryLock;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.json.JSONObject;

import com.openai.models.CompletionUsage;

public class DailyCostMonitor {

  private static final String DAILY_COST_ACCUMULATION_FILE = "daily_cost_accumulation.txt";

  // model prop keys
  private static final String MAX_DAILY_DOLLAR_COST_PROP_NAME = "MAX_DAILY_AI_EXPRESSION_DOLLAR_COST";
  private static final String PLAN_COST_PER_1M_INPUT_TOKENS_PROP_NAME = "COST_PER_1M_AI_INPUT_TOKENs";
  private static final String PLAN_COST_PER_1M_OUTPUT_TOKENS_PROP_NAME = "COST_PER_1M_AI_OUTPUT_TOKENS";

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
      .build();

  public static class CostExceededException extends RuntimeException { }

  private final Path _costMonitoringDir;
  private final Path _costMonitoringFile;

  private double _maxDailyDollarCost;
  private double _costPerInputToken;
  private double _costPerOutputToken;

  public DailyCostMonitor(WdkModel wdkModel) throws WdkModelException {
    _costMonitoringDir = AiExpressionCache.getAiExpressionCacheParentDir(wdkModel);
    _costMonitoringFile = _costMonitoringDir.resolve(DAILY_COST_ACCUMULATION_FILE);

    _maxDailyDollarCost = getNumberProp(wdkModel, MAX_DAILY_DOLLAR_COST_PROP_NAME);
    _costPerInputToken = getNumberProp(wdkModel, PLAN_COST_PER_1M_INPUT_TOKENS_PROP_NAME) / 1000000;
    _costPerOutputToken = getNumberProp(wdkModel, PLAN_COST_PER_1M_OUTPUT_TOKENS_PROP_NAME) / 1000000;
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

        try (Writer out = new FileWriter(_costMonitoringFile.toFile())) {
          out.write(new JSONObject()
              .put(JSON_DATE_PROP, newDate)
              .put(JSON_COST_PROP, newCost)
              .toString()
          );
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
    catch (IOException e) {
      throw new WdkRuntimeException("Unable to read AI expression cost file: " + _costMonitoringFile);
    }
  }

}
