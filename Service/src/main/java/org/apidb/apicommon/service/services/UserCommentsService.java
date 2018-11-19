package org.apidb.apicommon.service.services;

import org.apidb.apicommon.model.comment.pojo.Comment;
import org.apidb.apicommon.model.comment.pojo.CommentRequest;
import org.gusdb.wdk.core.api.JsonKeys;
import org.gusdb.wdk.model.WdkModelException;
import org.json.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;

@Path(UserCommentsService.BASE_PATH)
public class UserCommentsService extends AbstractUserCommentService {
  public static final String URI_PARAM = "comment-id";
  public static final String BASE_PATH = "/user-comments";
  public static final String ID_PATH   = "/{" + URI_PARAM + "}";

  @Context
  protected UriInfo _uriInfo;

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response newComment(CommentRequest body) throws WdkModelException {
    final long id = getCommentFactory().createComment(body,
        getWdkModel().getUserFactory().getUserById(body.getUserId()));

    return Response.created(_uriInfo.getAbsolutePathBuilder().build(id))
      .entity(new JSONObject().append(JsonKeys.ID, id))
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
  public Collection<Comment> getAllComments(
    @QueryParam("author")      final Long   author,
    @QueryParam("target-type") final String targetType,
    @QueryParam("target-id")   final String targetId
  ) throws WdkModelException {
    boolean isTargetId = targetId != null;
    boolean isTargetType = targetType != null;
    boolean isAuthor = author != null;

    if(!isTargetId && !isTargetType && !isAuthor)
      throw new BadRequestException("Comment filter required");

    if(isTargetId ^ isTargetType)
      throw new BadRequestException("target-id and target-type cannot be used separately");

    return getCommentFactory().queryComments(author, targetId, targetType);
  }

  @GET
  @Path(ID_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public Comment getComment(@PathParam(URI_PARAM) long _commentId) throws WdkModelException {
    return getCommentFactory().getComment(_commentId)
        .orElseThrow(NotFoundException::new);
  }

  @DELETE
  @Path(ID_PATH)
  public Response deleteComment(@PathParam(URI_PARAM) long _commentId) throws WdkModelException {
    checkCommentId(_commentId);

    getCommentFactory().deleteComment(_commentId);
    return Response.noContent().build();
  }
}
