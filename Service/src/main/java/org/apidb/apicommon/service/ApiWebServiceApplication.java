package org.apidb.apicommon.service;

import static org.gusdb.fgputil.functional.Functions.filter;

import java.util.Set;

import org.apidb.apicommon.service.services.*;
import org.apidb.apicommon.service.services.comments.UserCommentService;
import org.eupathdb.common.service.EuPathServiceApplication;
import org.gusdb.fgputil.SetBuilder;
import org.gusdb.wdk.service.service.SessionService;
import org.gusdb.wdk.service.service.user.BasketService;

public class ApiWebServiceApplication extends EuPathServiceApplication {

  @Override
  public Set<Class<?>> getClasses() {
    return new SetBuilder<Class<?>>()

      // add WDK services
      .addAll(filter(super.getClasses(), clazz ->
          !clazz.getName().equals(SessionService.class.getName()) &&
          !clazz.getName().equals(BasketService.class.getName())))

      // add ApiCommon-specific services
      .add(UserCommentsService.class)
      .add(UserCommentService.class)
      .add(TranscriptToggleService.class)
      .add(ApiSessionService.class)
      .add(CustomBasketService.class)
      .add(BigWigTrackService.class)
      .add(JBrowseService.class)

      .toSet();
  }
}
