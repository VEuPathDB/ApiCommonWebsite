/**
 A custom extension to the WDK's UserDB mbean, adding LDAP aliases attribute.
 Other franchises may not use LDAP (or Oracle) so this is not included in the
 WDK mbean.
 Note the mbean path is the same, so don't load both this mbean and the WDK mbean.
**/
package org.apidb.apicommon.jmx.mbeans.apidb;


public class UserDBExt extends AbstractDBExt implements UserDBExtMBean   {

  public UserDBExt() {
    super("UserPlatform");
  }

}
