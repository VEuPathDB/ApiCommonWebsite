package org.apidb.apicommon.jmx.mbeans.wdk;

import org.apidb.apicommon.model.CommentFactory;
import org.apidb.apicommon.controller.CommentActionUtility;

public class CommentConfig extends AbstractConfig {

  public CommentConfig() {
    super();
    init();
  }
  
  protected void init() {
    CommentFactory factory = null;
    try {
      factory = getCommentFactory();
      org.apidb.apicommon.model.CommentConfig commentConfig = factory.getCommentConfig();  
      setValuesFromGetters(null, commentConfig);
    } catch (Exception e) {
      //
    }
  }

  /**
    Derived from org.apidb.apicommon.controller.CommentAction
  */
  private CommentFactory getCommentFactory() throws Exception {
    return CommentActionUtility.getCommentFactory(context);
  }

}
