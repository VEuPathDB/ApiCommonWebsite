package org.apidb.apicommon.service.services.comments;

import org.apache.commons.io.IOUtils;
import org.apidb.apicommon.controller.MimeTypes;
import org.apidb.apicommon.model.comment.pojo.Attachment;
import org.apidb.apicommon.model.comment.pojo.Comment;
import org.apidb.apicommon.model.userfile.UserFile;
import org.apidb.apicommon.model.userfile.UserFileFactory;
import org.apidb.apicommon.model.userfile.UserFileUploadException;
import org.apidb.apicommon.service.services.AbstractUserCommentService;
import org.apidb.apicommon.service.services.UserCommentsService;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.runtime.InstanceManager;
import org.gusdb.wdk.core.api.JsonKeys;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.user.User;
import org.json.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

@Path(AttachmentsService.BASE_PATH)
public class AttachmentsService extends AbstractUserCommentService {
  public static final String BASE_PATH = UserCommentsService.BASE_PATH +
      UserCommentsService.ID_PATH + "/attachments";
  public static final String URI_PARAM = "attachment-id";
  public static final String ID_PATH   = "/{" + URI_PARAM + "}";

  @Context
  private UriInfo _uriInfo;

  /**
   * Get a list of all attachment details for a given comment.
   *
   * @param commentId ID of the comment for which the attachments will be looked
   *                  up.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<Attachment> getAllAttachments(
      @PathParam(UserCommentsService.URI_PARAM) long commentId)
      throws WdkModelException {
    checkCommentId(commentId);

    return getCommentFactory().getAllAttachments(commentId);
  }

  /**
   * Upload new attachment
   *
   * Note: A user must be logged in and own the target comment to use this
   *       endpoint.
   *
   * @param commentId   ID of the comment that the uploaded attachment will
   *                    belong to.
   * @param description User description/note for the current attachment upload.
   * @param fileStream  Input stream of bytes for the contents of the uploaded
   *                    file.
   * @param meta        Data about the uploaded file.
   */
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public Response newAttachment(
    @PathParam(UserCommentsService.URI_PARAM) final long commentId,
    @FormDataParam("description") final String description,
    @FormDataParam("file") final InputStream fileStream,
    @FormDataParam("file") final FormDataContentDisposition meta
  ) throws WdkModelException, UserFileUploadException {

    if(description == null)
      throw new BadRequestException("description is required");

    if(meta == null || fileStream == null)
      throw new BadRequestException("file cannot be null");

    final User user = fetchUser();
    checkCommentOwnership(fetchComment(commentId), user);

    UserFile userFile = buildUserFile(getWdkModel(), user, meta, description,
        fileStream);

    getFileFactory().addUserFile(userFile);
    // IMPORTANT: The "attachment" instance is created after the call to
    //            addUserFile due to the fact that addUserFile writes changes
    //            to the userFile instance.
    getCommentFactory().createAttachment(commentId,
        Attachment.fromUserFile(userFile));

    return Response.created(_uriInfo.getAbsolutePathBuilder().build())
      .entity(new JSONObject().put(JsonKeys.ID, userFile.getUserFileId()))
      .build();
  }

  /**
   * Delete comment attachment by ID
   *
   * Note: A user must be logged in and own the target comment to use this
   *       endpoint.
   *
   * @param commentId    ID of the comment the attachment belongs to
   * @param attachmentId ID of the attachment to be deleted
   */
  @DELETE
  @Path(ID_PATH)
  public Response deleteAttachment(
      @PathParam(UserCommentsService.URI_PARAM) long commentId,
      @PathParam(URI_PARAM) long attachmentId) throws WdkModelException {
    final Comment com = fetchComment(commentId);

    checkFileId(attachmentId);

    final User user = fetchUser();

    checkCommentOwnership(com, user);
    getCommentFactory().deleteAttachment(commentId, attachmentId);
    getFileFactory().deleteUserFile(attachmentId);
    return Response.noContent().build();
  }

  /**
   * Get attachment data by ID
   *
   * @param commentId    ID of the comment an attachment belongs to
   * @param attachmentId ID of the attachment to retrieve
   *
   * @return Response containing the attachment file as a byte stream.
   */
  @GET
  @Path(ID_PATH)
  public Response getAttachment(
      @PathParam(UserCommentsService.URI_PARAM) long commentId,
      @PathParam(URI_PARAM) long attachmentId) throws WdkModelException {
    final Attachment att = getCommentFactory()
        .getAttachment(commentId, attachmentId)
        .orElseThrow(NotFoundException::new);
    final InputStream data = getFileFactory().getUserFile(attachmentId);

    return Response.ok(
        (StreamingOutput) outputStream -> IOUtils.copy(data, outputStream),
        mimeTypeOf(att.getName()))
        .header("Content-Disposition", "attachment; filename=" + att.getName())
        .build();
  }

  private UserFileFactory getFileFactory() {
    return InstanceManager.getInstance(UserFileFactory.class, getWdkModel()
        .getProjectId());
  }

  private static UserFile buildUserFile(WdkModel wdk, User user,
      FormDataContentDisposition meta, String comment, InputStream data)
      throws WdkModelException {
    final String userUID = user.getSignature().trim();

    UserFile userFile = new UserFile(userUID);

    userFile.setFileName(meta.getFileName());
    try {
      userFile.setFileData(IoUtil.readAllBytes(data));
    } catch(IOException e) {
      throw new WdkModelException(e);
    }
    userFile.setContentType(meta.getType());
    userFile.setFileSize(meta.getSize());
    userFile.setEmail(user.getEmail());
    userFile.setUserUID(userUID);
    userFile.setTitle(meta.getFileName());
    userFile.setNotes(comment);
    userFile.setProjectName(wdk.getDisplayName());
    userFile.setProjectVersion(wdk.getVersion());

    return userFile;
  }

  private String mimeTypeOf(String name) {
    return MimeTypes.getMimeType(name.substring(name.lastIndexOf('.')));
  }

  private void checkFileId(long fileId) throws WdkModelException {
    if(!getFileFactory().fileExists(fileId))
      throw new NotFoundException();
  }
}
