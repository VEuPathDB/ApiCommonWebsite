package org.apidb.apicommon.service.services.comments;

import java.util.function.Supplier;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;

import org.apidb.apicommon.controller.CommentFactoryManager;
import org.apidb.apicommon.model.comment.CommentAlertEmailFormatter;
import org.apidb.apicommon.model.comment.CommentFactory;
import org.apidb.apicommon.model.comment.pojo.Comment;
import org.apidb.apicommon.model.comment.pojo.CommentRequest;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.user.User;
import org.gusdb.wdk.service.service.AbstractWdkService;

public abstract class AbstractUserCommentService extends AbstractWdkService {
  public static final String SOURCE_EMAIL     = "annotator@apidb.org";
  public static final String ANNOTATORS_EMAIL = "EUPATHDB_ANNOTATORS@lists.upenn.edu";
  public static final String REDMINE_EMAIL    = "redmine@apidb.org";

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

  /**
   * Sends the "new comment" alert emails (self-alert to the annotators list plus
   * the submitting user, and a redmine ticket alert) for a newly created comment.
   * Shared by any endpoint that creates a {@code comments} row.
   */
  protected void notificationEmail(WdkModel wdk, User user, CommentRequest com,
      long comId) throws WdkModelException {

    final CommentAlertEmailFormatter form = new CommentAlertEmailFormatter();

    final String subject = form.makeSubject(wdk.getProjectId(), com);
    final String url = getClientURL(comId, com.getTarget().getId(), com.getTarget().getType());
    final String smtp = wdk.getModelConfig().getSmtpServer();

    Utilities.sendEmail(smtp, ANNOTATORS_EMAIL + ", " + user.getEmail(),
        SOURCE_EMAIL, subject, form.makeSelfAlertBody(wdk, user, com, comId, url));
    Utilities.sendEmail(smtp, REDMINE_EMAIL, SOURCE_EMAIL, subject,
        form.makeRedmineAlertBody(wdk, user, com, comId, url));
  }

  private String getClientURL(long comId, String targetId, String targetType) {
    return getContextUri() +
      "/app/user-comments/show" +
      "?stableId="              + targetId +
      "&commentTargetId="       + targetType +
      "#"                       + comId;
  }
}
