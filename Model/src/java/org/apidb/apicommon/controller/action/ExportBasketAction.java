package org.apidb.apicommon.controller.action;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.gusdb.wdk.controller.action.ActionUtility;
import org.gusdb.wdk.model.jspwrap.UserBean;

public class ExportBasketAction extends Action {

    private static final String PARAM_TARGET_PROJECT = "target";
    private static final String PARAM_RECORD_CLASS = "recordClass";

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        UserBean user = ActionUtility.getUser(servlet, request);

        String targetProject = request.getParameter(PARAM_TARGET_PROJECT);
        String rcName = request.getParameter(PARAM_RECORD_CLASS);
        int count = user.exportBasket(targetProject, rcName);

        response.setContentType("application/json");
        PrintWriter writer = response.getWriter();
        writer.write(Integer.toString(count));
        writer.close();

        return null;
    }
}
