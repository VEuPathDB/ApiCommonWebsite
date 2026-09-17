package org.apidb.apicommon.service.services.jbrowse;

import javax.ws.rs.Path;

/**
 * This is a duplicate service to JBrowseService but with a different root path
 * segment to allow us to block guests from accessing /jbrowser services while
 * keeping open /jbrowse-apollo
 */
@Path("/jbrowse-apollo")
public class JBrowseApolloService extends JBrowseService {

}
