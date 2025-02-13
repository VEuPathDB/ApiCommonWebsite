package org.apidb.apicommon.model.report.ai;

import org.gusdb.wdk.model.report.AbstractReporter;
import org.gusdb.wdk.model.report.Reporter;
import org.gusdb.wdk.model.report.ReporterConfigException;
import org.apidb.apicommon.model.report.ai.expression.Summarizer;
import org.gusdb.wdk.model.WdkModelException;

import org.json.JSONObject;
import java.io.IOException;
import java.io.OutputStream;

public class SingleGeneAiExpressionReporter extends AbstractReporter {    

  private enum CacheMode {
    TEST("test"),
    POPULATE("populate");
    private final String mode;
    CacheMode(String mode) {
      this.mode = mode;
    }
    public String getMode() {
      return mode;
    }
    public static CacheMode fromString(String mode) throws IllegalArgumentException {
      for (CacheMode cm : CacheMode.values()) {
        if (cm.mode.equalsIgnoreCase(mode)) {
          return cm;
        }
      }
      throw new IllegalArgumentException("Invalid CacheMode: " + mode);
    }
  }

  private CacheMode _cacheMode = CacheMode.TEST;
    
  @Override
  public Reporter configure(JSONObject config) throws ReporterConfigException, WdkModelException {
    try {
	    _cacheMode = CacheMode.fromString(config.getString("cacheMode"));
    } catch (IllegalArgumentException e) {
	    throw new ReporterConfigException("Invalid cacheMode value: " + config.getString("cacheMode"), e);
    }
    return this;
  }

  @Override
  protected void write(OutputStream out) throws IOException, WdkModelException {

  }
  

}


