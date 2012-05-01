package org.apidb.apicommon.jmx.mbeans.wdk;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
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
import org.apache.log4j.Logger;

public class Properties extends BeanBase implements DynamicMBean {

  Map<String, String> props;
  @SuppressWarnings("unused")
private static final Logger logger = Logger.getLogger(Properties.class.getName());

  public Properties() {
    super();
    init();
  }

  public AttributeList getAttributes(String[] names) {
      AttributeList list = new AttributeList();
      for (String name : names) {
          String value = props.get(name);
          if (value != null)
              list.add(new Attribute(name, value));
      }
      return list;
  }

  public String getAttribute(String name) throws AttributeNotFoundException {
    String value = props.get(name);
    if (value != null)
      return value;
    else
      throw new AttributeNotFoundException("No such property: " + name);
  }

  public AttributeList setAttributes(AttributeList list) {
    AttributeList retlist = new AttributeList();
    Iterator<?> itr = list.iterator();
    while( itr.hasNext() ) {
      Attribute attr = (Attribute)itr.next();
      String name = attr.getName();
      Object value = attr.getValue();
      if (props.get(name) != null && value instanceof String) {
          props.put(name, (String) value);
          retlist.add(new Attribute(name, value));
      }
    }
    return retlist;
  }

  public void setAttribute(Attribute attribute) 
  throws InvalidAttributeValueException, MBeanException, AttributeNotFoundException {
    String name = attribute.getName();
    if (props.get(name) == null)
        throw new AttributeNotFoundException(name);
    Object value = attribute.getValue();
    if (!(value instanceof String)) {
        throw new InvalidAttributeValueException(
                "Attribute value not a string: " + value);
    }
    props.put(name, (String) value);
  }

  public MBeanInfo getMBeanInfo() {
    ArrayList<String> names = new ArrayList<String>();
    for (Object name : props.keySet()) {
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
              false,   // isWritable
              false);  // isIs
    }

    MBeanOperationInfo[] opers = {
      new MBeanOperationInfo(
              "reload",
              "Reload properties from model",
              null,
              "void",
              MBeanOperationInfo.ACTION)
    };

    return new MBeanInfo(
            this.getClass().getName(),
            "WDK Properties MBean",
            attrs,
            null,  // constructors
            opers,  // operators
            null); // notifications
  }

  public Object invoke(String name, Object[] args, String[] sig) 
  throws MBeanException, ReflectionException {
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

  protected void init() {
    props = wdkModel.getProperties();
  }


}
