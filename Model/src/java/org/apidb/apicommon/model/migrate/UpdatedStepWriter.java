package org.apidb.apicommon.model.migrate;

import java.sql.Types;
import java.util.Collection;

import org.gusdb.fgputil.ListBuilder;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.fix.table.TableRowInterfaces.TableRowWriter;
import org.gusdb.wdk.model.fix.table.steps.StepData;

public class UpdatedStepWriter implements TableRowWriter<StepData> {

  @Override
  public String getWriteSql(String schema) {
    return "insert into wdkmaint.wdk_updated_steps (step_id) values (?)";
  }

  @Override
  public Integer[] getParameterTypes() {
    return new Integer[]{ Types.INTEGER };
  }

  @Override
  public Collection<Object[]> toValues(StepData obj) {
    return ListBuilder.asList(new Object[]{ obj.getStepId() });
  }

  @Override
  public void setUp(WdkModel wdkModel) throws Exception {
    // nothing to do here
  }

  @Override
  public void tearDown(WdkModel wdkModel) throws Exception {
    // nothing to do here
  }

}
