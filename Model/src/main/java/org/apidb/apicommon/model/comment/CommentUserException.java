package org.apidb.apicommon.model.comment;

public class CommentUserException extends CommentException {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public CommentUserException() {
    super();
  }

  public CommentUserException(String message) {
    super(message);
  }

  public CommentUserException(Throwable cause) {
    super(cause);
  }

  public CommentUserException(String message, Throwable cause) {
    super(message, cause);
  }

  public CommentUserException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
