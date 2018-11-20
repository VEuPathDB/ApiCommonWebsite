package org.apidb.apicommon.service.services;

import joptsimple.internal.Strings;
import org.apidb.apicommon.model.comment.pojo.Comment;
import org.apidb.apicommon.model.comment.pojo.CommentRequest;
import org.gusdb.wdk.core.api.JsonKeys;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.user.User;
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
  public Response newComment(CommentRequest body) throws WdkModelException {
    final WdkModel wdk = getWdkModel();
    final User user = wdk.getUserFactory().getUserById(body.getUserId());
    final long id = getCommentFactory().createComment(body, user);

    notificationEmail(wdk, user, body, id);

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
  public Comment getComment(@PathParam(URI_PARAM) long _commentId)
      throws WdkModelException {
    return getCommentFactory().getComment(_commentId)
        .orElseThrow(NotFoundException::new);
  }

  @DELETE
  @Path(ID_PATH)
  public Response deleteComment(@PathParam(URI_PARAM) long _commentId)
      throws WdkModelException {
    checkCommentId(_commentId);
    getCommentFactory().deleteComment(_commentId);
    return Response.noContent().build();
  }

  private URI buildURL(long comId) {
    return _uriInfo.getAbsolutePathBuilder().build(comId);
  }

  private void notificationEmail(WdkModel wdk, User user, CommentRequest com,
      long comId) throws WdkModelException {

    final String projectId = wdk.getProjectId();
    final String organism  = com.getOrganism();
    final String targType  = com.getTarget().getType();
    final String stableId  = com.getTarget().getId();
    final String subject = String.format("%s %s %s", projectId, targType, stableId);

    StringBuilder body = new StringBuilder();
    if(projectId.equals("TriTrypDB") || organism.equals("Plasmodium falciparum") || organism.equals("Cryptosporidium parvum")) {
      body.append("Thank you! Your comment will be reviewed by a curator shortly.\n");
    } else {
      body.append("Thanks for your comment!\n");
    }
    body.append("-------------------------------------------------------\n")
        .append("Comment Id: ").append(comId).append("\n")
        .append("Headline: ").append(com.getHeadline()).append("\n")
        .append("Target: ").append(targType).append("\n")
        .append("Source_Id: ").append(stableId).append("\n")
        .append("Comment: ").append(com.getContent()).append("\n")
        .append("PMID: ").append(Strings.join(com.getPubMedIds(), ", ")).append("\n")
        .append("DOI(s): ").append(Strings.join(com.getDigitalObjectIds(), ", ")).append("\n")
        .append("Related Genes: ").append(Strings.join(com.getRelatedStableIds(), ", ")).append("\n")
        .append("Accession: ").append(Strings.join(com.getGenBankAccessions(), ", ")).append("\n")
        .append("Email: ").append(user.getEmail()).append("\n")
        .append("Organism: ").append(organism).append("\n")
        .append("DB Name: ").append(com.getExternalDb().getName()).append("\n")
        .append("DB Version: ").append(com.getExternalDb().getVersion()).append("\n")
        .append("Comment Link: ").append(buildURL(comId).toString()).append("\n")
        .append("-------------------------------------------------------\n");

    // used for redmine issue tracker
    StringBuilder bodyRedmine = new StringBuilder(body.toString())
        .append("Project: uiresulvb\n")
        .append("Tracker: Communication\n")
        .append("Assignee: annotator\n")
        .append("EuPathDB Team: Outreach\n")
        .append("Component:").append(projectId).append("\n");


    final String smtp = wdk.getModelConfig().getSmtpServer();
    Utilities.sendEmail(smtp, ANNOTATORS_EMAIL + ", " + user.getEmail(),
        SOURCE_EMAIL, subject, body.toString());
    Utilities.sendEmail(smtp, REDMINE_EMAIL, SOURCE_EMAIL, subject,
        bodyRedmine.toString());
  }
}
