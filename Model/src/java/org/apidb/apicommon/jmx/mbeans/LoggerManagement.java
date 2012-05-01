package org.apidb.apicommon.jmx.mbeans;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Enumeration;


public class LoggerManagement implements DynamicMBean {
  private static Logger logger = Logger.getLogger(LoggerManagement.class);

  Map<String, String> managedLoggers;

  public LoggerManagement() {
  }

  protected void init() {
    managedLoggers = getManagedLoggers();
  }

  private Map<String, String> getManagedLoggers() {
    logger.debug("getManagedLoggers ");

	HashMap<String, String> loggerMap = new HashMap<String, String>();
	
	// Get the root logger
	Logger rootLogger = LogManager.getRootLogger();
	String rootLoggerName = rootLogger.getName();
	String rootLoggerLevel = rootLogger.getEffectiveLevel().toString();
	loggerMap.put(rootLoggerName, rootLoggerLevel);
	
	// Get all other loggers
	Enumeration<?> e = LogManager.getCurrentLoggers();	
	while (e.hasMoreElements()) {
		Logger managedLogger = (Logger) e.nextElement();
		String loggerName = managedLogger.getName();
		String level = managedLogger.getEffectiveLevel().toString();
		loggerMap.put(loggerName, level);
	}
	
	return loggerMap;
  }

  public AttributeList getAttributes(String[] loggerNames) {
    logger.debug("getAttributes ");
    
    // refresh the values. Note that this does not change the mbean interface even if loggers
    // are added or removed. The interface is only determined when getMbeanInfo() is called.
    // This is important for cases when the log levels are changed by means other than this interface.
    managedLoggers = getManagedLoggers();

    AttributeList attrs = new AttributeList();
    for (String loggerName : loggerNames) {
        String value = managedLoggers.get(loggerName);
        if (value != null)
            attrs.add(new Attribute(loggerName, value));
    }
    return attrs;
  }
  
  public String getAttribute(String loggerName) throws AttributeNotFoundException {
    logger.debug("getAttribute " + loggerName);
    String value = managedLoggers.get(loggerName);
    if (value != null)
      return value;
    else
      throw new AttributeNotFoundException("No such property: " + loggerName);  
  }
  
  public MBeanInfo getMBeanInfo() {
    logger.debug("getMBeanInfo ");

    // loggers can change as new classes are loaded by the application, so we reinitialize
    // whenever getMBeanInfo() is called. This can mean a different interface across invocations
    // as loggers are added and removed.
    // This changing interface may not be completely kosher (depending on how I read the spec)
    // but I think it's ok since we only change the interface for an agent when it asks for it.
    // If this proves troublesome, pull it out and expect the agent to invoke the 'reload' 
    // Operation.
    // Future: see jmx.mbean.info.changed Notification in Java 7. And immutableInfo field in
    // javax.management.Descriptor.
    init();
    
    ArrayList<String> names = new ArrayList<String>();
    for (Object name : managedLoggers.keySet()) {
      names.add((String) name);
    }
    
    MBeanAttributeInfo[] attrs = new MBeanAttributeInfo[names.size()];
    Iterator<String> it = names.iterator();
    for (int i = 0; i < attrs.length; i++) {
      String name = it.next();
      attrs[i] = new MBeanAttributeInfo(
              name,
              "java.lang.String",
              name,
              true,    // isReadable
              true,   // isWritable
              false);  // isIs
    }

    MBeanOperationInfo[] opers = {
      new MBeanOperationInfo(
              "reload",
              "Reload managed loggers",
              null,
              "void",
              MBeanOperationInfo.ACTION)
    };

    return new MBeanInfo(
            this.getClass().getName(),
            "Log4J loggers MBean",
            attrs,
            null,  // constructors
            opers, // operators
            null); // notifications
    
  }
  
  public Object invoke(String name, Object[] args, String[] sig) 
  throws MBeanException, ReflectionException {
    logger.debug("invoke " + sig);
    if (name.equals("reload") &&
            (args == null || args.length == 0) &&
            (sig == null || sig.length == 0)) {
        try {
          init();
          return null;
        } catch (Exception e) {
          throw new MBeanException(e);
        }
    }
    throw new ReflectionException(new NoSuchMethodException(name));
  }
  
  public void setAttribute(Attribute attribute)
  throws InvalidAttributeValueException, MBeanException, AttributeNotFoundException {

   String name = attribute.getName();

   logger.debug("setAttribute " + name);

   if (managedLoggers.get(name) == null)
        throw new AttributeNotFoundException(name);

    Object value = attribute.getValue();    
    if (!(value instanceof String)) {
        throw new InvalidAttributeValueException(
                "Attribute value not a string: " + value);
    }
    String levelName = (String) value;
    
    Logger thisLogger = Logger.getLogger(name);
    Level newLevel = Level.toLevel(levelName, Level.INFO);

    thisLogger.setLevel(newLevel);
    managedLoggers = getManagedLoggers();
  }
  
  public AttributeList setAttributes(AttributeList list) {
    logger.debug("setAttributes ");

    AttributeList retlist = new AttributeList();
    Iterator<?> itr = list.iterator();

    while( itr.hasNext() ) {
      Attribute attr = (Attribute)itr.next();
      String name = attr.getName();
      Object value = attr.getValue();
      if (managedLoggers.get(name) != null && value instanceof String) {
          managedLoggers.put(name, (String) value);
          retlist.add(new Attribute(name, value));
      }
    }

    return retlist;  
  }


}