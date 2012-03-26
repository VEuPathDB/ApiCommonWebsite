package org.apidb.apicommon.model.view;

import java.util.ArrayList;
import java.util.List;

public class Isolate {

    private String country;
    private String type;
    private int total;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
