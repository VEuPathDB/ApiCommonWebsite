package org.apidb.apicommon.jmx.mbeans.wdk;

import java.util.ArrayList;

public interface AbstractDBMBean {
  public void refresh();
  // TODO: remove suppression when revisiting this project
  @SuppressWarnings("rawtypes")
  public ArrayList getDblinkList();
  public String getglobal_name();
  public String getversion();
  public String getserver_name();
  public String getserver_ip();
  public String getsystem_date();
  public String getlogin();
  public String getservice_name();
  public String getdb_name();
  public String getdb_unique_name();
  public String getinstance_name();
  public String getdb_domain();
  public String getclient_host();
  public String getos_user();
  public String getcurrent_userid();
  public String getsession_user();
  public String getsession_userid();
}


