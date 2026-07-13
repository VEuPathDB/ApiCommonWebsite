package org.apidb.apicommon.service;

import java.util.Set;

import org.apidb.apicommon.service.filter.ApiCheckLoginFilter;
import org.apidb.apicommon.service.services.ApiBasketService;
import org.apidb.apicommon.service.services.ApiProjectService;
import org.apidb.apicommon.service.services.ApiRecordService;
import org.apidb.apicommon.service.services.ApiSessionService;
import org.apidb.apicommon.service.services.ApiStepService;
import org.apidb.apicommon.service.services.OrganismMetricsService;
import org.apidb.apicommon.service.services.TranscriptToggleService;
import org.apidb.apicommon.service.services.comments.AttachmentsService;
import org.apidb.apicommon.service.services.comments.UserCommentsService;
import org.apidb.apicommon.service.services.dataPlotter.ProfileSetService;
import org.apidb.apicommon.service.services.jbrowse.JBrowse2Service;
import org.apidb.apicommon.service.services.jbrowse.JBrowseService;
import org.apidb.apicommon.service.services.jbrowse.JBrowseUserDatasetsService;
import org.eupathdb.common.service.EuPathServiceApplication;
import org.gusdb.fgputil.SetBuilder;
import org.gusdb.wdk.service.filter.CheckLoginFilter;
import org.gusdb.wdk.service.service.ProjectService;
import org.gusdb.wdk.service.service.RecordService;
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
      .replace(BasketService.class, ApiBasketService.class)
      .replace(StepService.class, ApiStepService.class)
      .replace(ProjectService.class, ApiProjectService.class)
      .replace(CheckLoginFilter.class, ApiCheckLoginFilter.class)
      .replace(SessionService.class, ApiSessionService.class)
      .replace(RecordService.class, ApiRecordService.class)

      // add ApiCommon-specific services
      .add(AttachmentsService.class)
      .add(UserCommentsService.class)
      .add(TranscriptToggleService.class)
      .add(JBrowseService.class)
      .add(JBrowse2Service.class)
      .add(JBrowseUserDatasetsService.class)
      .add(ProfileSetService.class)
      .add(OrganismMetricsService.class)

      .toSet();
  }
}
