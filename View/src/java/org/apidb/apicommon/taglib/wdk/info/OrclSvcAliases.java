package org.apidb.apicommon.taglib.wdk.info;

import org.apidb.apicommon.taglib.wdk.WdkTagBase;
import javax.servlet.jsp.JspException;
import java.util.*;
import javax.naming.*;
import javax.naming.directory.*;
import java.util.ArrayList;

public class OrclSvcAliases extends WdkTagBase {
    
    private String var;
    private String servicename;
    private ArrayList<String> names = new ArrayList<String>();
    
    public void doTag() throws JspException {
        super.doTag();
        queryLdap();
        this.getRequest().setAttribute(var, this);
    }
    
    public String getNames() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            sb.append(names.get(i));
            if (i < names.size() - 1) {
                sb.append(", ");
            }
        }

        return sb.toString();
    }
    
    public void setServicename(String servicename) {
        this.servicename =  servicename;
    }

    public void setVar(String var) {
        this.var = var;
    }

    private void queryLdap() throws JspException {
        try {
            Hashtable env = new Hashtable();
    
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            String ldapUrl = "ldap://ds1.apidb.org " +
                             "ldap://ds2.apidb.org " +
                             "ldap://ds3.apidb.org";
            env.put(Context.PROVIDER_URL, ldapUrl);
            DirContext dctx = new InitialDirContext(env);
            
            String base = "cn=OracleContext,ou=applications,dc=apidb,dc=org";
            SearchControls sc = new SearchControls();
            String[] attributeFilter = { "cn" };
            sc.setReturningAttributes(attributeFilter);
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            
            String filter = "(&(orclNetDescString=*" + hostname + "*)(orclNetDescString=*SERVICE_NAME" + servicename + "*))";

            NamingEnumeration results = dctx.search(base, filter, sc);
            while (results.hasMore()) {
              SearchResult sr = (SearchResult) results.next();
              Attributes attrs = sr.getAttributes();
        
              Attribute attr = attrs.get("cn");
              names.add((String)attr.get());
            }
            dctx.close();
         } catch (NamingException ne) {
            throw new JspException(ne);
         }
      }
}
