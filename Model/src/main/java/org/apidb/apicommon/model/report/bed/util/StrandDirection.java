package org.apidb.apicommon.model.report.bed.util;
import java.lang.IllegalArgumentException;
import java.lang.IllegalStateException;

public enum StrandDirection {

  forward("+"),
  reverse("-"),
  none(".");

  private final String _sign;
  private StrandDirection(String sign) {
    _sign = sign;
  }
  public String getSign() {
    return _sign;
  }

  public static StrandDirection fromSign(String sign) {
    if("+".equals(sign)){
      return StrandDirection.forward;
    } else if ("-".equals(sign)){
      return StrandDirection.reverse;
    } else if (".".equals(sign)){
      return StrandDirection.none;
    } else {
      throw new IllegalArgumentException(sign);
    }
  }

  public static StrandDirection fromEfOrEr(String efOrEr) {
    if("f".equals(efOrEr)){
      return StrandDirection.forward;
    } else if ("r".equals(efOrEr)){
      return StrandDirection.reverse;
    } else {
      throw new IllegalArgumentException(efOrEr);
    }
  }

  public StrandDirection opposite(){
    switch(this){
      case forward:
        return StrandDirection.reverse;
      case reverse:
        return StrandDirection.forward;
    }
    throw new IllegalStateException(this.toString());
  }

}
