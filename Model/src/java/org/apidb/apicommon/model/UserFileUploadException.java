package org.apidb.apicommon.model;


public class UserFileUploadException extends Exception {

    public UserFileUploadException() {
      super();
    }

    public UserFileUploadException(String msg) {
      super(msg);
    }

    public UserFileUploadException(Throwable cause) {
      super(cause);
    }

    public String formatErrors() {
      String message = super.getMessage();
      return "UserFileUploadException: " + message;
    }
    
    public String getMessage() {
        return formatErrors();
    }

}
