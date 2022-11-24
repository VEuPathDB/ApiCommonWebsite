package org.apidb.apicommon.model.report.bed.util;
import java.lang.IllegalArgumentException;
import java.lang.IllegalStateException;

public enum StrandDirection {

  forward("+"),
  reverse("-");

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
    } else {
      throw new IllegalArgumentException(sign);
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
