/*

Call all the getter methods in ModelConfig, put values in a Map with keys
derived from getter method name - remove 'get', lowercase first letter - 
and typically the same as the attribute names in the model-config.xml. 

Password values (properties having the string 'password') are masked.

Tag requires 'var' to name an instance of this class in the JSP page scope. 
The map is available via getProps().

Example usage:

<%@ taglib prefix="api" uri="http://apidb.org/taglib" %>
<api:modelConfig var="c"/>
${c.props['authenticationConnectionUrl']}

<!-- sample iteration ->
<c:forEach var="cfg" items="${modelConfig.props}">
    ${cfg.key} = ${cfg.value}<br>
</c:forEach>

*/
package org.apidb.apicommon.taglib.wdk.info;

import org.apidb.apicommon.taglib.wdk.WdkTagBase;
import org.gusdb.wdk.model.ModelConfig;

import java.util.Map;
import java.util.HashMap;
import java.lang.Class;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import javax.servlet.jsp.JspException;

public class ModelConfigTag extends WdkTagBase {
    
    private String var;
    private ModelConfig modelConfig;
    public HashMap props;
    
    public void doTag() throws JspException {
        super.doTag();
        modelConfig = wdkModel.getModelConfig();    
        setValuesFromModelConfigGetters();
        this.getRequest().setAttribute(var, this);
   }
   
    public Map getProps() {
        return props;
    }
   
    public void setValuesFromModelConfigGetters() throws JspException {
      props = new HashMap();
      try {
        Class c = Class.forName(modelConfig.getClass().getName());
        Method[] methods = c.getMethods();
          for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            String mname = method.getName();
            if (method.getDeclaringClass() == c && mname.startsWith("get")) {
              // remove 'get', lowercase first letter
              String key = Character.toLowerCase(mname.charAt(3)) + mname.substring(4);
              Object value = method.invoke(modelConfig);
              
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