package org.apidb.apicommon.model.comment.pojo;

public class MultiBox {

    private String name;
    private Integer value;

    public MultiBox(String name, Integer value) {
      this.name = name;
      this.value = value;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }

    public void setValue(Integer value) {
      this.value = value;
    }

    public Integer getValue() {
      return this.value;
    }
}
