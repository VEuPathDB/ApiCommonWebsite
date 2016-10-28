package org.apidb.apicommon.model.userdataset;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.gusdb.wdk.model.user.dataset.UserDataset;
import org.gusdb.wdk.model.user.dataset.UserDatasetCompatibility;
import org.gusdb.wdk.model.user.dataset.UserDatasetType;
import org.gusdb.wdk.model.user.dataset.UserDatasetTypeHandler;

public class GeneListTypeHandler extends UserDatasetTypeHandler {

  @Override
  public UserDatasetCompatibility getCompatibility(UserDataset userDataset, DataSource appDbDataSource) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public UserDatasetType getUserDatasetType() {
    // TODO Auto-generated method stub
    return new UserDatasetType("GeneList", "1.0");
  }

  @Override
  public String[] getInstallInAppDbCommand(UserDataset userDataset, Map<String, Path> fileNameToTempFileMap) {
    String[] cmd = {"installGeneListUserDataset", userDataset.getUserDatasetId().toString(), fileNameToTempFileMap.get("genelist.txt").toString()};
    return cmd;
  }

  @Override
  public Set<String> getInstallInAppDbFileNames(UserDataset userDataset) {
    Set<String> filenames = new HashSet<String>();
    filenames.add("genelist.txt");
    return filenames;
  }

  @Override
  public String[] getUninstallInAppDbCommand(UserDataset userDataset) {
    String[] cmd = {"installGeneListUserDataset", userDataset.getUserDatasetId().toString()};
    return cmd;
  }

}
