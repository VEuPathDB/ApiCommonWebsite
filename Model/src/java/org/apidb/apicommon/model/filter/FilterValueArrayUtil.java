package org.apidb.apicommon.model.filter;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FilterValueArrayUtil {

  public static JSONObject getFilterValueArray(String... values) {
    JSONObject jsValue = new JSONObject();
    JSONArray jsArray = new JSONArray();
    for (String value : values) {
      jsArray.put(value);
    }
    jsValue.put("values", jsArray);
    return jsValue;
  }

  public static Set<String> getStringSetFromValueArray(JSONObject jsValue) throws JSONException {
    JSONArray jsArray = jsValue.getJSONArray("values");
    Set<String> set = new HashSet<String>();
    for (int i = 0; i < jsArray.length(); i++) {
      String value = jsArray.getString(i);
      set.add(value);
    }
    return set;
  }
}
