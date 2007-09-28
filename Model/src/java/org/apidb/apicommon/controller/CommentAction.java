package org.apidb.apicommon.controller;

import java.io.File;

import javax.servlet.ServletContext;

import org.apache.struts.action.Action;
import org.apidb.apicommon.model.CommentFactory;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModelException;

public abstract class CommentAction extends Action {

    public static final String LOCATION_COORDINATETYPE_PROTEIN = "protein";
    public static final String LOCATION_COORDINATETYPE_GENOME = "genome";

    protected CommentFactory getCommentFactory() throws WdkModelException {
        CommentFactory factory = null;
        try {
            factory = CommentFactory.getInstance();
        } catch (WdkModelException ex) {
            // the comment factory is not initialized yet, do it
            ServletContext application = getServlet().getServletContext();

            // get the gus_home & project id
            String gusHome = application.getRealPath(application.getInitParameter(Utilities.SYSTEM_PROPERTY_GUS_HOME));
            String projectId = application.getInitParameter(Utilities.ARGUMENT_PROJECT_ID);
            
            File configFile = new File(gusHome + "/config/" + projectId + "/comment-config.xml");
            CommentFactory.initialize(configFile);
            factory = CommentFactory.getInstance();
        }
        return factory;
    }
}
