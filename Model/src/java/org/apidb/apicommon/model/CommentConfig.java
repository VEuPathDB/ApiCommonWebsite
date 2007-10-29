/**
 * 
 */
package org.apidb.apicommon.model;

/**
 * @author xingao
 * 
 */
public class CommentConfig {

    private String platformClass;
    private String connectionUrl;
    private String login;
    private String password;
    private String commentSchema = "";
    private int initialSize;
    private int maxActive;
    private int maxIdle;
    private int minIdle;
    private int maxWait;
    private String commentTextFileDir;
    private String userLoginSchema = "";
    private String userLoginDbLink = "";

    /**
     * @return Returns the platformClass.
     */
    public String getPlatformClass() {
        return platformClass;
    }

    /**
     * @param platformClass
     *          The platformClass to set.
     */
    public void setPlatformClass(String platformClass) {
        this.platformClass = platformClass;
    }

    /**
     * @return Returns the commentSchema.
     */
    public String getCommentSchema() {
        return commentSchema;
    }

    /**
     * @param commentSchema
     *          The commentSchema to set.
     */
    public void setCommentSchema(String commentSchema) {
        if (commentSchema != null && commentSchema.length() > 0) {
            this.commentSchema = (commentSchema.endsWith(".")) ? commentSchema
                    : commentSchema + ".";
        }
    }

    /**
     * @return Returns the connectionUrl.
     */
    public String getConnectionUrl() {
        return connectionUrl;
    }

    /**
     * @param connectionUrl
     *          The connectionUrl to set.
     */
    public void setConnectionUrl(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }

    /**
     * @return Returns the login.
     */
    public String getLogin() {
        return login;
    }

    /**
     * @param login
     *          The login to set.
     */
    public void setLogin(String login) {
        this.login = login;
    }

    /**
     * @return Returns the password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password
     *          The password to set.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return Returns the initialSize.
     */
    public int getInitialSize() {
        return initialSize;
    }

    /**
     * @param initialSize
     *          The initialSize to set.
     */
    public void setInitialSize(int initialSize) {
        this.initialSize = initialSize;
    }

    /**
     * @return Returns the maxActive.
     */
    public int getMaxActive() {
        return maxActive;
    }

    /**
     * @param maxActive
     *          The maxActive to set.
     */
    public void setMaxActive(int maxActive) {
        this.maxActive = maxActive;
    }

    /**
     * @return Returns the maxIdle.
     */
    public int getMaxIdle() {
        return maxIdle;
    }

    /**
     * @param maxIdle
     *          The maxIdle to set.
     */
    public void setMaxIdle(int maxIdle) {
        this.maxIdle = maxIdle;
    }

    /**
     * @return Returns the maxWait.
     */
    public int getMaxWait() {
        return maxWait;
    }

    /**
     * @param maxWait
     *          The maxWait to set.
     */
    public void setMaxWait(int maxWait) {
        this.maxWait = maxWait;
    }

    /**
     * @return Returns the minIdle.
     */
    public int getMinIdle() {
        return minIdle;
    }

    /**
     * @param minIdle
     *          The minIdle to set.
     */
    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    public String getCommentTextFileDir() {
        return commentTextFileDir;
    }

    public void setCommentTextFileDir(String commentTextFile) {
        this.commentTextFileDir = commentTextFile;
    }

    public String getUserLoginSchema() {
        return userLoginSchema;
    }

    public void setUserLoginSchema(String userLoginSchema) {
        if (userLoginSchema != null && userLoginSchema.length() > 0) {
            this.userLoginSchema = (userLoginSchema.endsWith(".")) ? userLoginSchema
                    : userLoginSchema + ".";
        }
    }

    /**
     * @return the userLoginDbLink
     */
    public String getUserLoginDbLink() {
        return userLoginDbLink;
    }

    /**
     * @param userLoginDbLink
     *          the userLoginDbLink to set
     */
    public void setUserLoginDbLink(String userLoginDbLink) {
        if (userLoginDbLink != null && userLoginDbLink.length() > 0) {
            this.userLoginDbLink = (userLoginDbLink.startsWith("@")) ? userLoginDbLink
                    : "@" + userLoginDbLink;
        }
    }
}
