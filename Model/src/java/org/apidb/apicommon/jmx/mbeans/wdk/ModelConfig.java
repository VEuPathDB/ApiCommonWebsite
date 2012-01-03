package org.apidb.apicommon.jmx.mbeans.wdk;

public class ModelConfig extends AbstractConfig {

  public ModelConfig() {
    super();
    init();
  }
  
  protected void init() {
    org.gusdb.wdk.model.ModelConfig       modelConfig       = wdkModel.getModelConfig();
    org.gusdb.wdk.model.ModelConfigUserDB modelConfigUserDB = modelConfig.getUserDB();
    org.gusdb.wdk.model.ModelConfigAppDB  modelConfigAppDB  = modelConfig.getAppDB();

    
    setValuesFromGetters("global", modelConfig);
    setValuesFromGetters("userDb", modelConfigUserDB);
    setValuesFromGetters("appDb",  modelConfigAppDB);
  }

}