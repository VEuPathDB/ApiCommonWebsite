package org.apidb.apicommon.service;

import java.util.Set;

import org.apidb.apicommon.service.services.ApiBasketService;
import org.apidb.apicommon.service.services.ApiProjectService;
import org.apidb.apicommon.service.services.ApiSessionService;
import org.apidb.apicommon.service.services.ApiStepService;
import org.apidb.apicommon.service.services.BigWigTrackService;
import org.apidb.apicommon.service.services.TranscriptToggleService;
import org.apidb.apicommon.service.services.comments.AttachmentsService;
import org.apidb.apicommon.service.services.comments.UserCommentsService;
import org.apidb.apicommon.service.services.jbrowse.JBrowseService;
import org.apidb.apicommon.service.services.jbrowse.JBrowseUserDatasetsService;
import org.eupathdb.common.service.EuPathServiceApplication;
import org.gusdb.fgputil.SetBuilder;
import org.gusdb.wdk.service.service.ProjectService;
import org.gusdb.wdk.service.service.SessionService;
import org.gusdb.wdk.service.service.user.BasketService;
import org.gusdb.wdk.service.service.user.StepService;
public class ApiWebServiceApplication extends EuPathServiceApplication {

  @Override
  public Set<Class<?>> getClasses() {
    return new SetBuilder<Class<?>>()

      // add WDK services
      .addAll(super.getClasses())

      // replace overridden services with ApiCommon versions
      .replace(SessionService.class, ApiSessionService.class)
      .replace(BasketService.class, ApiBasketService.class)
      .replace(StepService.class, ApiStepService.class)
      .replace(ProjectService.class, ApiProjectService.class)

      // add ApiCommon-specific services
      .add(AttachmentsService.class)
      .add(UserCommentsService.class)
      .add(TranscriptToggleService.class)
      .add(BigWigTrackService.class)
      .add(JBrowseService.class)
      .add(JBrowseUserDatasetsService.class)

      .toSet();
  }
}
