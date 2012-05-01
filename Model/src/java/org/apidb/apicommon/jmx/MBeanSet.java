package org.apidb.apicommon.jmx;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

public class MBeanSet {

  public static final Map<String, String> mbeanClassNames = makeMap();

  private static Map<String, String> makeMap() {

    Map<String, String> map = new HashMap<String, String>() {
    	private static final long serialVersionUID = -1L;
    	{
    		put("org.apidb.apicommon.jmx.mbeans.wdk.Meta",          "org.gusdb.wdk:type=Meta");
    		put("org.apidb.apicommon.jmx.mbeans.wdk.Cache",         "org.gusdb.wdk:type=Cache");
    		put("org.apidb.apicommon.jmx.mbeans.wdk.ModelConfig",   "org.gusdb.wdk:type=ModelConfig");
    		put("org.apidb.apicommon.jmx.mbeans.wdk.CommentConfig", "org.gusdb.wdk:type=CommentConfig");
    		put("org.apidb.apicommon.jmx.mbeans.wdk.Properties",    "org.gusdb.wdk:type=Properties");
    		put("org.apidb.apicommon.jmx.mbeans.apidb.UserDBExt",   "org.apidb.wdk:group=Databases,type=UserDB");
    		put("org.apidb.apicommon.jmx.mbeans.apidb.AppDBExt",    "org.apidb.wdk:group=Databases,type=AppDB");
    		put("org.apidb.apicommon.jmx.mbeans.LoggerManagement",  "org.apidb.wdk:type=Log4J");
    	}
    };

    return Collections.unmodifiableMap(map);
  
  }

}