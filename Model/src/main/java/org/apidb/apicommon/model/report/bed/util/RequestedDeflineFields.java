package org.apidb.apicommon.model.report.bed.util;

import java.util.HashSet;
import org.json.JSONObject;
import org.json.JSONArray;

public class RequestedDeflineFields extends HashSet<String> {
  
  public RequestedDeflineFields(JSONObject config){
    super();
    if(config.optString("deflineType","short").equals("full")){
      JSONArray a = config.getJSONArray("deflineFields");
      for(int i = 0; i < a.length(); i++){
        super.add(a.getString(i));
      }
    }
  }
}
