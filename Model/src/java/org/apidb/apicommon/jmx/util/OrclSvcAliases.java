package org.apidb.apicommon.jmx.util;

import org.apache.log4j.Logger;
import java.util.*;
import javax.naming.*;
import javax.naming.directory.*;
import java.util.ArrayList;

public class OrclSvcAliases {
    
    private String servicename;
    private ArrayList<String> namelist = new ArrayList<String>();
    private static final Logger logger = Logger.getLogger(OrclSvcAliases.class);
    
    public OrclSvcAliases() {
        queryLdap();
    }
    
    public OrclSvcAliases(String servicename) {
        this.servicename = servicename;
        queryLdap();
    }
    
    public String getNames() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < namelist.size(); i++) {
            sb.append(namelist.get(i));
            if (i < namelist.size() - 1) {
                sb.append(", ");
            }
        }

        return sb.toString();
    }

    public ArrayList<String> getNameArray() {
        return namelist;
    }
    
    public void setServicename(String servicename) {
        this.servicename =  servicename;
    }

    private void queryLdap() {
        try {
            Hashtable<String, String> env = new Hashtable<String, String>();
    
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            String ldapUrl = "ldap://ds2.apidb.org " +
                             "ldap://ds4.apidb.org ";
            env.put(Context.PROVIDER_URL, ldapUrl);
            DirContext dctx = new InitialDirContext(env);
            
            String base = "cn=OracleContext,ou=applications,dc=apidb,dc=org";
            SearchControls sc = new SearchControls();
            String[] attributeFilter = { "cn" };
            sc.setReturningAttributes(attributeFilter);
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            
            String filter = "(orclNetDescString=*" + servicename + "*)";

            NamingEnumeration<SearchResult> results = dctx.search(base, filter, sc);
            while (results.hasMore()) {
              SearchResult sr = results.next();
              Attributes attrs = sr.getAttributes();
        
              Attribute attr = attrs.get("cn");
              namelist.add((String)attr.get());
            }
            dctx.close();
         } catch (NamingException ne) {
            logger.warn(ne);
         }
      }
}
