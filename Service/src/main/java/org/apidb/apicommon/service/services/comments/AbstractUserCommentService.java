package org.apidb.apicommon.service.services.comments;

import java.util.function.Supplier;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;

import org.apidb.apicommon.controller.CommentFactoryManager;
import org.apidb.apicommon.model.comment.CommentFactory;
import org.apidb.apicommon.model.comment.pojo.Comment;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.user.User;
import org.gusdb.wdk.service.service.AbstractWdkService;

public abstract class AbstractUserCommentService extends AbstractWdkService {
  protected CommentFactory getCommentFactory() {
    return CommentFactoryManager.getCommentFactory(getWdkModel().getProjectId());
  }

  protected User fetchUser() {
    final User out = getRequestingUser();
    if (out.isGuest())
      throw new NotAuthorizedException("you must login before performing this action");
    return out;
  }

  protected void checkCommentId(long commentId) throws WdkModelException {
    if(!getCommentFactory().commentExists(commentId))
      throw new NotFoundException();
  }

  protected void checkCommentOwnership(Comment com, User user) {
    if(com.getUserId() != user.getUserId())
      throw new NotAuthorizedException("cannot modify another user's comment");
  }

  protected Comment fetchComment(long commentId) throws WdkModelException {
    return getCommentFactory()
        .getComment(commentId)
        .orElseThrow(commentNotFound(commentId));
  }

  protected Supplier<NotFoundException> commentNotFound(long id) {
    return () -> new NotFoundException(
        String.format("user comment %d not found", id));
  }
}
