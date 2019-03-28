package org.apidb.apicommon.service;

import static org.gusdb.fgputil.functional.Functions.filter;

import java.util.Set;

import org.apidb.apicommon.service.services.ApiBasketService;
import org.apidb.apicommon.service.services.ApiSessionService;
import org.apidb.apicommon.service.services.ApiStepService;
import org.apidb.apicommon.service.services.BigWigTrackService;
import org.apidb.apicommon.service.services.JBrowseService;
import org.apidb.apicommon.service.services.TranscriptToggleService;
import org.apidb.apicommon.service.services.UserCommentsService;
import org.apidb.apicommon.service.services.comments.AttachmentsService;
import org.eupathdb.common.service.EuPathServiceApplication;
import org.gusdb.fgputil.SetBuilder;
import org.gusdb.wdk.service.service.SessionService;
import org.gusdb.wdk.service.service.user.BasketService;
import org.gusdb.wdk.service.service.user.StepService;
public class ApiWebServiceApplication extends EuPathServiceApplication {

  @Override
  public Set<Class<?>> getClasses() {
    return new SetBuilder<Class<?>>()

      // add WDK services
      .addAll(filter(super.getClasses(), clazz ->
          !clazz.getName().equals(SessionService.class.getName()) &&
          !clazz.getName().equals(BasketService.class.getName()) &&
          !clazz.getName().equals(StepService.class.getName())))

      // add ApiCommon-specific services
      .add(ApiSessionService.class)
      .add(ApiBasketService.class)
      .add(ApiStepService.class)
      .add(AttachmentsService.class)
      .add(UserCommentsService.class)
      .add(TranscriptToggleService.class)
      .add(BigWigTrackService.class)
      .add(JBrowseService.class)

      .toSet();
  }
}
