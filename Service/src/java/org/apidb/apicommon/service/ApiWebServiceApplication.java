package org.apidb.apicommon.service;

import java.util.Set;

import org.apidb.apicommon.service.services.TranscriptToggleService;
import org.gusdb.fgputil.SetBuilder;
import org.gusdb.wdk.service.WdkServiceApplication;

public class ApiWebServiceApplication extends WdkServiceApplication {

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
