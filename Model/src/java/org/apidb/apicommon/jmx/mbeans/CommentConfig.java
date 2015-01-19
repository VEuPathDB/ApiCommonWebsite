package org.apidb.apicommon.jmx.mbeans;

import org.gusdb.wdk.jmx.mbeans.AbstractAttributesBean;
import org.apidb.apicommon.model.comment.CommentFactory;
import org.apidb.apicommon.controller.CommentActionUtility;
import org.apache.log4j.Logger;

/**
 * A view of the WDK's representation of comment-config.xml.
 * The configuration data comes from WDK class instances, not directly 
 * from configuation files on the filesystem, so it's important to note
 * that the WDK may have added or removed or even changed some values
 * relative to the state on the filesystem.
 *
 * @see org.gusdb.wdk.jmx.mbeans.AbstractConfig#setValuesFromGetters
 * @see org.apidb.apicommon.model.comment.CommentConfig
 */
public class CommentConfig extends AbstractAttributesBean {

  private static final Logger logger = Logger.getLogger(CommentConfig.class);

  public CommentConfig() {
    super();
    init();
  }

  @Override
  protected void init() {
    CommentFactory factory = null;
    try {
      factory = getCommentFactory();
      org.apidb.apicommon.model.comment.CommentConfig commentConfig = factory.getCommentConfig();  
      setValuesFromGetters(null, commentConfig);
    } catch (Exception e) {
      logger.error("MBean Load Error ", e);
    }
  }

  /**
    Derived from org.apidb.apicommon.controller.CommentAction
  */
  private CommentFactory getCommentFactory() throws Exception {
    return CommentActionUtility.getCommentFactory(getContext());
  }

}
