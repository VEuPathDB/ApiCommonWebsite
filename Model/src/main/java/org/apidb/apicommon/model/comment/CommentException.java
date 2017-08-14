package org.apidb.apicommon.model.comment;

public abstract class CommentException extends Exception {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public CommentException() {
    super();
  }

  public CommentException(String message) {
    super(message);
  }

  public CommentException(Throwable cause) {
    super(cause);
  }

  public CommentException(String message, Throwable cause) {
    super(message, cause);
  }

  public CommentException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
