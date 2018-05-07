package org.apidb.apicommon.model.report.summaryview;

import org.json.JSONWriter;

public class Isolate {

    private String country;
    private String gaz;
    private String type;
    private int total;
    private double lat;
    private double lng;
    
    /**
     * serialize this region to a json writer
     * @param writer
     */
    protected void writeJson(JSONWriter writer) {
      writer.object();
      writer.key("country").value(country);
      writer.key("gaz").value(gaz);
      writer.key("type").value(type);
      writer.key("total").value(total);
      writer.key("lat").value(lat);
      writer.key("lng").value(lng);
      writer.endObject();
    }

    public Isolate(String country) {
        this.country = country;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getGaz() {
        return gaz;
    }

    public void setGaz(String gaz) {
        this.gaz = gaz;
    }
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }


}
