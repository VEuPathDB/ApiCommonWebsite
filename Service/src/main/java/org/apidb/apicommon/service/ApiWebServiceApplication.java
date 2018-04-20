package org.apidb.apicommon.service;

import static org.gusdb.fgputil.functional.Functions.filter;

import java.util.Set;

import org.apidb.apicommon.service.services.ApiSessionService;
import org.apidb.apicommon.service.services.BigWigTrackService;
import org.apidb.apicommon.service.services.CustomBasketService;
import org.apidb.apicommon.service.services.TranscriptToggleService;
import org.eupathdb.common.service.EuPathServiceApplication;
import org.gusdb.fgputil.SetBuilder;
import org.gusdb.wdk.service.service.SessionService;
import org.gusdb.wdk.service.service.user.BasketService;
import org.apidb.apicommon.service.services.JBrowseService;

public class ApiWebServiceApplication extends EuPathServiceApplication {
 
  @Override
  public Set<Class<?>> getClasses() {
    return new SetBuilder<Class<?>>()

      // add WDK services
      .addAll(filter(super.getClasses(), clazz ->
          !clazz.getName().equals(SessionService.class.getName()) &&
          !clazz.getName().equals(BasketService.class.getName())))

      // add ApiCommon-specific services
      .add(TranscriptToggleService.class)
      .add(ApiSessionService.class)
      .add(CustomBasketService.class)
      .add(BigWigTrackService.class)
      .add(JBrowseService.class)

      .toSet();
  }
}
