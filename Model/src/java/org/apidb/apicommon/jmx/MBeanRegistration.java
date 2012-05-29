package org.apidb.apicommon.jmx;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.servlet.ServletContext;

import org.apache.log4j.Logger;

/**
 * Registers mbeans named in MBeanSet.mbeanClassNames.
 */
public class MBeanRegistration {

  private static final Logger logger = Logger.getLogger(MBeanRegistration.class);
  private MBeanServer server;
  private List<ObjectName> registeredMBeans = new ArrayList<ObjectName>();
  
  private Map<String, String> mbeanClassNames = MBeanSet.mbeanClassNames;


  public MBeanRegistration() {
  }

  /** 
   * Initialization and registration.
   */
  public void init() {
    server = getMBeanServer();
    registerMBeans();  
  }

  /** 
   * Unregister mbeans and cleanup
   */
  public void destroy() {
      unregisterMBeans();
  }

  private void registerMBeans() {
    try {
      for (Map.Entry<String, String> entry : mbeanClassNames.entrySet()) {
      //for (String name : mbeanClassNames) {
        Object mb = getMbeanClassForName(entry.getKey());
        if (mb == null) throw new RuntimeException("unable to instantiate class for " + entry.getKey());
        ObjectName objectName = makeObjectName(entry.getValue());
        registerMBean(mb, objectName);
      }
    } catch (MalformedObjectNameException mone) {
        throw new RuntimeException(mone);
    } catch (MBeanRegistrationException mbre) {
        throw new RuntimeException(mbre);    
    } catch (InstanceAlreadyExistsException iaee) {
        logger.error(iaee);
    } catch (NotCompliantMBeanException ncmbe) {
        throw new RuntimeException(ncmbe);
    }
  }

  private void registerMBean(Object pObject, ObjectName pName)
    throws MalformedObjectNameException, MBeanRegistrationException, InstanceAlreadyExistsException, NotCompliantMBeanException {
    logger.debug("registering mbean " + pName.toString());
    server.registerMBean(pObject, pName);
    registeredMBeans.add(pName);
  }

  private void unregisterMBeans() {
    for (ObjectName name : registeredMBeans) {
      try {
        logger.debug("unregistering mbean " + name.toString());
        server.unregisterMBean(name);
      } catch (InstanceNotFoundException infe) {
        logger.warn("Exception while unregistering " + name + " " + infe);
      } catch (MBeanRegistrationException mbre) {
        logger.warn("Exception while unregistering " + name + " " + mbre);
      }
    }
  }

  private Object getMbeanClassForName(String classname) {
    Object bean = null;
    Class<?> clazz = getClass(classname);
    if (clazz == null) return null;
    try {
      Constructor<?> con = clazz.getConstructor();
      bean = con.newInstance();
    } catch (InstantiationException ie) {
        logger.warn(ie);
    } catch (IllegalAccessException iae) {
        logger.warn(iae);
    } catch (InvocationTargetException ite) {
        logger.warn(ite);
    } catch (NoSuchMethodException nsme) {
        logger.warn(nsme);
    }
    return bean;
  }

  private Class<?> getClass(String classname) {
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      return Class.forName(classname, false, loader);
    } catch (ClassNotFoundException cnfe) {
      logger.warn(cnfe);
    }
    return null;
  }

  private ObjectName makeObjectName(String pObjectNameString) throws MalformedObjectNameException, NullPointerException { 
    String context = getContextName();
    String objectNameString = pObjectNameString + ",context=" + context;
    return new ObjectName(objectNameString);
  }

  private String getContextName() {
    ServletContext sc = ContextThreadLocal.get();
    String contextName = null;

    if (sc.getMajorVersion() > 2 || sc.getMajorVersion() == 2 && sc.getMinorVersion() >= 5) {
      // Servlet API is >= 2.5 and has ServletContext getContextPath() method but 
      // we have to make an indiret method call so code compiles for API < 2.5
      try {
        Method m = sc.getClass().getMethod("getContextPath", new Class[] {});
        contextName = (String) m.invoke(sc, (Class<?>) null);
      } catch (SecurityException se) {
        throw new RuntimeException(se);
      } catch (NoSuchMethodException nsme) {
        throw new RuntimeException(nsme);
      } catch (IllegalArgumentException iae) {
        throw new RuntimeException(iae);
      } catch (IllegalAccessException iae) {
        throw new RuntimeException(iae);
      } catch (InvocationTargetException ite) {
        throw new RuntimeException(ite);
      }
    } else {
      // hack for old servlet API: use the name of the tempdir
      String tmpdir = ((java.io.File) sc.getAttribute("javax.servlet.context.tempdir")).getName();
      contextName = "/" + tmpdir;
    }

    return contextName;
  }

  private MBeanServer getMBeanServer() {
    MBeanServer mbserver = null;

    ArrayList<MBeanServer> mbservers = MBeanServerFactory.findMBeanServer(null);

    if (mbservers.size() > 0) {
      mbserver = mbservers.get(0);
    }

    if (mbserver == null) {
      mbserver = MBeanServerFactory.createMBeanServer();
    } 

    return mbserver;
  }


}