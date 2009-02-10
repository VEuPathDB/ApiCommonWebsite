/*

Call all the getter methods in CommentConfig, put values in a Map with keys
derived from getter method name - remove 'get', lowercase first letter - 
and typically the same as the attribute names in the model-config.xml. 

Password values (properties having the string 'password') are masked.

Tag requires 'var' to name an instance of this class in the JSP page scope. 
The map is available via getProps().

Example usage:

<%@ taglib prefix="api" uri="http://apidb.org/taglib" %>
<api:commentConfig var="c"/>
${c.props['authenticationConnectionUrl']}

<!-- sample iteration ->
<c:forEach var="cfg" items="${commentConfig.props}">
    ${cfg.key} = ${cfg.value}<br>
</c:forEach>

*/
package org.apidb.apicommon.taglib.wdk.info;

import org.apidb.apicommon.taglib.wdk.WdkTagBase;
import org.apidb.apicommon.model.Comment;
import org.apidb.apicommon.model.CommentFactory;
import org.apidb.apicommon.model.CommentConfig;
import org.apidb.apicommon.controller.CommentAction;

import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModelException;

import java.util.Map;
import java.util.HashMap;
import java.lang.Class;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import javax.servlet.ServletContext;
import javax.servlet.jsp.JspException;
import org.apache.log4j.Logger;

public class CommentConfigTag extends WdkTagBase {
    
    private String var;
    private CommentConfig commentConfig;
    private CommentFactory factory;
    public HashMap props;

    private Logger logger = Logger.getLogger(ModelConfigTag.class);
    
    public void doTag() throws JspException {
        super.doTag();
        try {
            CommentFactory factory = getCommentFactory();
        } catch (Exception e) {
            throw new JspException(e);
        }
        commentConfig = factory.getCommentConfig();
        setValuesFromCommentConfigGetters();
        this.getRequest().setAttribute(var, this);
   }

    /**
      Derived from org.apidb.apicommon.controller.CommentAction
    */
    private CommentFactory getCommentFactory() throws Exception {
        factory = null;
        try {
            factory = CommentFactory.getInstance();
        } catch (WdkModelException ex) {
            ServletContext application = getApplication();
            String gusHome = application.getRealPath(application.getInitParameter(Utilities.SYSTEM_PROPERTY_GUS_HOME));
            String projectId = application.getInitParameter(Utilities.ARGUMENT_PROJECT_ID);

            CommentFactory.initialize(gusHome, projectId);
            factory = CommentFactory.getInstance();
        }
        return factory;
    }
    
    public Map getProps() {
        return props;
    }
   
    public void setValuesFromCommentConfigGetters() throws JspException {
      props = new HashMap();
      try {
        Class c = Class.forName(commentConfig.getClass().getName());
        Method[] methods = c.getMethods();
          for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            String mname = method.getName();
            if ((method.getDeclaringClass().getName().startsWith("org.gusdb.wdk.model.") ||
                method.getDeclaringClass().getName().startsWith("org.apidb.apicommon.model."))
                && mname.startsWith("get")) {
              // remove 'get', lowercase first letter
              String key = Character.toLowerCase(mname.charAt(3)) + mname.substring(4);
              Object value = method.invoke(commentConfig);

              if ( value == null ||
                   !(value.getClass().getName().startsWith("java.lang.")) ) 
                continue;
              
              if ( (key.toLowerCase().contains("password") || 
                    key.toLowerCase().contains("passwd") )
                    && value instanceof String
                 ) { value = "*****"; }
              props.put(key, value);
            }
          }
      } catch (Exception e) {
        throw new JspException(e);
      }
    }

    public void setVar(String var) {
        this.var = var;
    }
}