package org.apidb.apicommon.model.migrate;

import java.io.IOException;
import java.util.List;

import org.eupathdb.common.fix.UpdatedStepWriter;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.fix.table.TableRowInterfaces.RowResult;
import org.gusdb.wdk.model.fix.table.TableRowInterfaces.TableRowUpdaterPlugin;
import org.gusdb.wdk.model.fix.table.TableRowInterfaces.TableRowWriter;
import org.gusdb.wdk.model.fix.table.TableRowUpdater;
import org.gusdb.wdk.model.fix.table.steps.StepData;
import org.gusdb.wdk.model.fix.table.steps.StepDataFactory;
import org.gusdb.wdk.model.fix.table.steps.StepDataWriter;
import org.gusdb.wdk.model.fix.table.steps.StepQuestionUpdater;

/**
 * Simple updater plugin that reads a question mapping file and updates all Steps question names in DB.  This
 * plugin only updates the "question_name" column and will not change text inside display_params.
 * 
 * @author rdoherty
 */
public class StepQuestionUpdaterPlugin implements TableRowUpdaterPlugin<StepData> {

  private StepQuestionUpdater _qNameUpdater;

  @Override
  public void configure(WdkModel wdkModel, List<String> args) throws IOException {
    if (args.size() != 1) {
      throw new IllegalArgumentException("Missing arguments.  Plugin args: <question_map_file>");
    }
    _qNameUpdater = new StepQuestionUpdater(args.get(0), false);
  }

  @Override
  public TableRowUpdater<StepData> getTableRowUpdater(WdkModel wdkModel) {
    return new TableRowUpdater<StepData>(new StepDataFactory(false), getWriterList(), this, wdkModel);
  }

  private List<TableRowWriter<StepData>> getWriterList() {
    return new ListBuilder<TableRowWriter<StepData>>()
        .add(new StepDataWriter())
        .add(new UpdatedStepWriter())
        .toList();
  }

  @Override
  public RowResult<StepData> processRecord(StepData step) throws Exception {
    return new RowResult<>(step).setShouldWrite(_qNameUpdater.updateQuestionName(step));
  }

  @Override
  public void dumpStatistics() { }

}
