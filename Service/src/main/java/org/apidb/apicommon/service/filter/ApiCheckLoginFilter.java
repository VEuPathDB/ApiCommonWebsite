package org.apidb.apicommon.service.filter;

import javax.annotation.Priority;

import org.gusdb.wdk.service.filter.CheckLoginFilter;

@Priority(30)
public class ApiCheckLoginFilter extends CheckLoginFilter {

  @Override
  protected boolean isGuestUserAllowed(String path) {
    if ((path.startsWith("jbrowse") && !path.startsWith("jbrowse2")) || path.startsWith("profileSet")) {
      return false;
    }
    return super.isGuestUserAllowed(path);
  }
}
