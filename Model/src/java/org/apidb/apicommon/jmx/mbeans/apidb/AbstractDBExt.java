/**
 A custom extension to the WDK's AbstractDB mbean, adding LDAP aliases attribute.
 Other franchises may not use LDAP (or Oracle) so this is not included in the
 WDK mbean.
**/
package org.apidb.apicommon.jmx.mbeans.apidb;

import org.apidb.apicommon.jmx.mbeans.wdk.AbstractDB;
import org.apidb.apicommon.jmx.util.OrclSvcAliases;

public class AbstractDBExt extends AbstractDB  {

  OrclSvcAliases osa;

  public AbstractDBExt(String type) {
    super(type);
    this.osa = getOrclSvcAliases();
  }

  public String getaliases_from_ldap() {
    return osa.getNames();
  }

  public void refresh() { 
    super.refresh();
    this.osa = getOrclSvcAliases();
  }

  protected OrclSvcAliases getOrclSvcAliases() {
    OrclSvcAliases osa = new OrclSvcAliases(getservice_name());
    return osa;
  }


}
