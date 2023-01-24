package org.apidb.apicommon.model.report.bed.util;

import java.lang.IllegalArgumentException;
import java.lang.IllegalStateException;
import java.util.function.Function;

public enum StrandDirection {

  forward("+", "f"),
  reverse("-", "r"),
  none(".", null);

  private final String _sign;
  private final String _fOrR;

  private StrandDirection(String sign, String fOrR) {
    _sign = sign;
    _fOrR = fOrR;
  }

  public String getSign() {
    return _sign;
  }

  private String getFOrR() {
    return _fOrR;
  }

  public StrandDirection opposite(){
    switch(this){
      case forward:
        return StrandDirection.reverse;
      case reverse:
        return StrandDirection.forward;
      default:
        throw new IllegalStateException("opposite called on value that does not have an opposite: " + name());
    }
  }

  public static StrandDirection fromSign(String sign) {
    return getByProperty(sign, StrandDirection::getSign);
  }

  public static StrandDirection fromEfOrEr(String fOrR) {
    return getByProperty(fOrR, StrandDirection::getFOrR);
  }

  private static StrandDirection getByProperty(String propValue, Function<StrandDirection, String> getter) {
    for (StrandDirection dir : values()) {
      String value = getter.apply(dir);
      if (value != null && value.equals(propValue)) {
        return dir;
      }
    }
    throw new IllegalArgumentException(propValue);
  }



}
