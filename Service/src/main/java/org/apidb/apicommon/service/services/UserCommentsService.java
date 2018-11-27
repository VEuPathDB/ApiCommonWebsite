package org.apidb.apicommon.service.services;

import org.apidb.apicommon.model.comment.CommentAlertEmailFormatter;
import org.apidb.apicommon.model.comment.pojo.Comment;
import org.apidb.apicommon.model.comment.pojo.CommentRequest;
import org.gusdb.wdk.core.api.JsonKeys;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.user.User;
import org.gusdb.wdk.service.annotation.InSchema;
import org.gusdb.wdk.service.annotation.OutSchema;
import org.json.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collection;

@Path(UserCommentsService.BASE_PATH)
public class UserCommentsService extends AbstractUserCommentService {
  public static final String URI_PARAM = "comment-id";
  public static final String BASE_PATH = "/user-comments";
  public static final String ID_PATH   = "/{" + URI_PARAM + "}";

  public static final String SOURCE_EMAIL     = "annotator@apidb.org";
  public static final String ANNOTATORS_EMAIL = "EUPATHDB_ANNOTATORS@lists.upenn.edu";
  public static final String REDMINE_EMAIL    = "redmine@apidb.org";

  @Context
  protected UriInfo _uriInfo;

  /**
   * Create (or "update") a user comment.
   *
   * Due to the structure and triggers in the user comment database, comment
   * records are only updated to toggle their visibility.  "Update" actions are
   * performed by first creating a new comment with a payload which includes the
   * optional "previousCommentId" field.  This will create a link between the
   * old and new "updated" form of the comment.  Attachments will be copied from
   * the old to the new.  A separate "delete" call will be needed from the
   * client to hide the previous comment.
   *
   * @param body JSON payload containing the data necessary to create a user
   *             comment.
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @InSchema("apicomm.user-comments.post-request")
  @OutSchema("apicomm.user-comments.post-response")
  public Response newComment(CommentRequest body) throws WdkModelException {
    final User user = fetchUser();

    // If the user is attempting to "update" or replace a
    // comment, ensure that the previous comment exists and
    // user actually owns that comment.
    if (body.getPreviousCommentId() != null)
      checkCommentOwnership(fetchComment(body.getPreviousCommentId()), user);

    final long id = getCommentFactory().createComment(body, user);

    notificationEmail(getWdkModel(), user, body, id);

    return Response.created(buildURL(id))
      .entity(new JSONObject().put(JsonKeys.ID, id))
      .build();
  }

  /**
   * List Comments
   *
   * NOTE: Returning all records (no filters) has been intentionally disallowed
   *       as it is a lot of data with no use case.
   *
   *       This endpoint will return a bad request error if no filters have been
   *       provided.
   *
   * @param author     Comment author id filter
   * @param targetType Comment target type filter.  Must be used in conjunction
   *                   with targetId
   * @param targetId   Comment target stable id.  Must be used in conjunction
   *                   with targetType
   *
   * @return List of filtered comments.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @OutSchema("apicomm.user-comments.get-response")
  public Collection<Comment> getAllComments(
    @QueryParam("author")      final Long   author,
    @QueryParam("target-type") final String targetType,
    @QueryParam("target-id")   final String targetId
  ) throws WdkModelException {
    boolean isTargetId   = targetId != null;
    boolean isTargetType = targetType != null;
    boolean isAuthor     = author != null;

    if(!isTargetId && !isTargetType && !isAuthor)
      throw new BadRequestException("Comment filter required");

    if(isTargetId ^ isTargetType)
      throw new BadRequestException("target-id and target-type cannot be used separately");

    return getCommentFactory().queryComments(author, targetId, targetType);
  }

  @GET
  @Path(ID_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @OutSchema("apicomm.user-comments.id.get-response")
  public Comment getComment(@PathParam(URI_PARAM) long comId) throws WdkModelException {
    return fetchComment(comId);
  }

  @DELETE
  @Path(ID_PATH)
  public Response deleteComment(@PathParam(URI_PARAM) long comId) throws WdkModelException {
    checkCommentOwnership(fetchComment(comId), fetchUser());
    getCommentFactory().deleteComment(comId);
    return Response.noContent().build();
  }

  private URI buildURL(long comId) {
    return _uriInfo.getAbsolutePathBuilder()
      .path(String.valueOf(comId))
      .build();
  }

  private void notificationEmail(WdkModel wdk, User user, CommentRequest com,
      long comId) throws WdkModelException {

    final CommentAlertEmailFormatter form = new CommentAlertEmailFormatter();

    final String subject = form.makeSubject(wdk.getProjectId(), com);
    final String url = buildURL(comId).toString();
    final String smtp = wdk.getModelConfig().getSmtpServer();

    Utilities.sendEmail(smtp, ANNOTATORS_EMAIL + ", " + user.getEmail(),
        SOURCE_EMAIL, subject, form.makeSelfAlertBody(wdk, user, com, comId, url));
    Utilities.sendEmail(smtp, REDMINE_EMAIL, SOURCE_EMAIL, subject,
        form.makeRedmineAlertBody(wdk, user, com, comId, url));
  }
}
