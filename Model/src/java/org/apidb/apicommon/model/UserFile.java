package org.apidb.apicommon.model;

import java.util.Date;


public class UserFile {
    
    private String userUID;
    private int userFileId;
    private String fileName;
    private byte[] fileData;
    private int fileSize;
    private String checksum;
    private Date uploadTime;
    private String title;
    private String notes;
    private String contentType;
    private String email;
    private String projectName;
    private String projectVersion;
    
    public UserFile(String userUID) {
        this.userUID = userUID;
    }

    public int getUserFileId() {
        return userFileId;
    }
    public void setUserFileId(int userFileId) {
        this.userFileId = userFileId;
    }
    
    public String getFileName() {
        return fileName;
    }
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public byte[] getFileData() {
        return fileData;
    }
    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }
    
    public int getFileSize() {
        return fileSize;
    }
    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getChecksum() {
        return checksum;
    }
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
    
    public Date getUploadTime() {
        return uploadTime;
    }
    public void setUploadTime(Date uploadTime) {
        this.uploadTime = uploadTime;
    }
    
    public String getUserUID() {
        return userUID;
    }
    public void setUserUID(String userUID) {
        this.userUID = userUID;
    }
    
    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public String getContentType() {
        return contentType;
    }
    public void setContentType(String email) {
        this.email = email;
    }

    public String getEmail() {
        return contentType;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getProjectName() {
        return projectName;
    }
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectVersion() {
        return projectVersion;
    }
    public void setProjectVersion(String projectVersion) {
        this.projectVersion = projectVersion;
    }

}