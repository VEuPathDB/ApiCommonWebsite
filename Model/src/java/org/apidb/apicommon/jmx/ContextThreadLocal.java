package org.apidb.apicommon.jmx;

import javax.servlet.ServletContext;

public class ContextThreadLocal {

  public static final ThreadLocal<ServletContext> wdkThreadLocal = new ThreadLocal<ServletContext>();

  public static void set(ServletContext sc) {
    wdkThreadLocal.set(sc);
  }

  public static void unset() {
    wdkThreadLocal.remove();
  }

  public static ServletContext get() {
    return (ServletContext)wdkThreadLocal.get();
  }
}