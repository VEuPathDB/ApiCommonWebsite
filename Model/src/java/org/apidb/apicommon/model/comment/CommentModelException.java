package org.apidb.apicommon.model.comment;

public class CommentModelException extends CommentException {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public CommentModelException() {
    super();
  }

  public CommentModelException(String message) {
    super(message);
  }

  public CommentModelException(Throwable cause) {
    super(cause);
  }

  public CommentModelException(String message, Throwable cause) {
    super(message, cause);
  }

  public CommentModelException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
