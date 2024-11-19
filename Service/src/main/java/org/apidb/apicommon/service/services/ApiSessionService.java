package org.apidb.apicommon.service.services;

import org.eupathdb.common.model.MultiBlastServiceUtil;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.user.User;
import org.gusdb.wdk.service.service.SessionService;

public class ApiSessionService extends SessionService {

  @Override
  protected void transferOwnership(User oldUser, User newUser, WdkModel wdkModel) throws WdkModelException {

    // transfer strategies and datasets
    super.transferOwnership(oldUser, newUser, wdkModel);

    // also transfer multi-blast jobs
    MultiBlastServiceUtil.transferMultiBlastJobs(oldUser, newUser, wdkModel, this);
  }
}
