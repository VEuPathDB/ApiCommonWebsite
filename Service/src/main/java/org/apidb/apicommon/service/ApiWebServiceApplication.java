package org.apidb.apicommon.service;

import java.util.Set;

import org.apidb.apicommon.service.services.ApiSessionService;
import org.apidb.apicommon.service.services.BigWigTrackService;
import org.apidb.apicommon.service.services.CustomBasketService;
import org.apidb.apicommon.service.services.JBrowseService;
import org.apidb.apicommon.service.services.JBrowseUserDatasetsService;
import org.apidb.apicommon.service.services.TranscriptToggleService;
import org.apidb.apicommon.service.services.UserCommentsService;
import org.apidb.apicommon.service.services.comments.AttachmentsService;
import org.eupathdb.common.service.EuPathServiceApplication;
import org.gusdb.fgputil.SetBuilder;
import org.gusdb.wdk.service.service.SessionService;
import org.gusdb.wdk.service.service.user.BasketService;

public class ApiWebServiceApplication extends EuPathServiceApplication {

  @Override
  public Set<Class<?>> getClasses() {
    return new SetBuilder<Class<?>>()

      // add WDK services
      .addAll(super.getClasses())

      // replace overridden services with ApiCommon versions
      .replace(SessionService.class, ApiSessionService.class)
      .replace(BasketService.class, CustomBasketService.class)

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
