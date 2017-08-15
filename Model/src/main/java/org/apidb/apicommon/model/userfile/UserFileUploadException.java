package org.apidb.apicommon.model.userfile;

public class UserFileUploadException extends Exception {

    private static final long serialVersionUID = 4296968079019764478L;

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

    @Override
    public String getMessage() {
        return formatErrors();
    }

}
