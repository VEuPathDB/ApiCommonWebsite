package org.apidb.apicommon.service.filter;

import java.io.IOException;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.PreMatching;

import org.gusdb.fgputil.web.RequestData;
import org.gusdb.wdk.controller.ContextLookup;
import org.gusdb.wdk.service.filter.CheckLoginFilter;

@PreMatching
@Priority(200)
public class ApiCheckLoginFilter extends CheckLoginFilter {

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {

    // gather request data to check URI
    RequestData request = ContextLookup.getRequest(_servletRequest.get(), _grizzlyRequest.get());

    // skip this filter if request is to jbrowse service
    if (!request.getFullRequestUri().contains("service/jbrowse")) {
      super.filter(requestContext);
    }

  }
}
