package org.apidb.apicommon.model.comment;

import org.gusdb.wdk.model.config.ModelConfigDB;

import java.util.Objects;

/**
 * @author xingao
 */
public class CommentConfig extends ModelConfigDB {

  private String commentSchema = "";
  private String commentTextFileDir;
  private String userLoginDbLink = "";
  private String userFileSchema = "";
  private String userFileUploadDir = "";

  /**
   * @return Returns the commentSchema.
   */
  public String getCommentSchema() {
    return commentSchema;
  }

  /**
   * @param commentSchema The commentSchema to set.
   */
  public void setCommentSchema(String commentSchema) {
    Objects.requireNonNull(commentSchema);
    if (commentSchema.length() > 0) {
      this.commentSchema = commentSchema;
    }
  }

  public String getCommentTextFileDir() {
    return commentTextFileDir;
  }

  public void setCommentTextFileDir(String commentTextFile) {
    this.commentTextFileDir = commentTextFile;
  }

  /**
   * @return the userLoginDbLink
   */
  public String getUserLoginDbLink() {
    return userLoginDbLink;
  }

  /**
   * @param userLoginDbLink the userLoginDbLink to set
   */
  public void setUserLoginDbLink(String userLoginDbLink) {
    if (userLoginDbLink != null && userLoginDbLink.length() > 0) {
      this.userLoginDbLink =
          (userLoginDbLink.startsWith("@")) ? userLoginDbLink :
              "@" + userLoginDbLink;
    }
  }

  public String getUserFileSchema() {
    return userFileSchema;
  }

  public void setUserFileSchema(String userFileSchema) {
    if (userFileSchema != null && userFileSchema.length() > 0) {
      this.userFileSchema = (userFileSchema.endsWith(".")) ? userFileSchema :
          userFileSchema + ".";
    }
  }

  public String getUserFileUploadDir() {
    return userFileUploadDir;
  }

  public void setUserFileUploadDir(String userFileUploadDir) {
    this.userFileUploadDir = userFileUploadDir;
  }

}
