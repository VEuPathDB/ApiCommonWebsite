package org.apidb.apicommon.service;

import java.util.Set;

import org.apidb.apicommon.service.services.TranscriptToggleService;
import org.eupathdb.common.service.EuPathServiceApplication;
import org.gusdb.fgputil.SetBuilder;

public class ApiWebServiceApplication extends EuPathServiceApplication {

  @Override
  public Set<Class<?>> getClasses() {
    return new SetBuilder<Class<?>>()

      // add WDK services
      .addAll(super.getClasses())

      // add ApiCommon-specific services
      .add(TranscriptToggleService.class)

      .toSet();
  }
}
