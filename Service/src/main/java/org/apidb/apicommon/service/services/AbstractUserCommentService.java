package org.apidb.apicommon.service.services;

import org.apidb.apicommon.controller.CommentFactoryManager;
import org.apidb.apicommon.model.comment.CommentFactory;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.service.service.AbstractWdkService;

import javax.ws.rs.NotFoundException;

public abstract class AbstractUserCommentService extends AbstractWdkService {
  protected CommentFactory getCommentFactory() {
    return CommentFactoryManager.getCommentFactory(getWdkModel().getProjectId());
  }

  protected void checkCommentId(long commentId) throws WdkModelException {
    if(!getCommentFactory().commentExists(commentId))
      throw new NotFoundException();
  }
}
