package org.apidb.apicommon.model;

import  org.apidb.apicommon.controller.UserFileUploadForm;

import org.apache.log4j.Logger;

import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.dbms.DBPlatform;
import org.gusdb.wdk.model.dbms.SqlUtils;

import org.json.JSONException;
import org.xml.sax.SAXException;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.security.NoSuchAlgorithmException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import java.io.*;
import java.lang.Process;
import java.lang.ProcessBuilder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class UserFileFactory {

    private static UserFileFactory factory;
    private Logger logger = Logger.getLogger(UserFileFactory.class);
    private DBPlatform platform;
    private CommentConfig config;

    public static void initialize(String gusHome, String projectId)
            throws NoSuchAlgorithmException, WdkModelException,
            ParserConfigurationException, TransformerFactoryConfigurationError,
            TransformerException, IOException, SAXException, SQLException,
            JSONException, WdkUserException, InstantiationException,
            IllegalAccessException, ClassNotFoundException {
        WdkModel wdkModel = WdkModel.construct(projectId, gusHome);

        // parse and load the configuration
        CommentConfigParser parser = new CommentConfigParser(gusHome);
        CommentConfig config = parser.parseConfig(projectId);

        // create a platform object
        DBPlatform platform = (DBPlatform) Class.forName(
                config.getPlatformClass()).newInstance();
        platform.initialize(wdkModel, "Comment", config);

        // create a factory instance
        factory = new UserFileFactory(platform, config);
    }

    public static UserFileFactory getInstance() throws WdkModelException {
        if (factory == null)
            throw new WdkModelException(
                    "Please initialize the factory properly.");
        return factory;
    }

    private UserFileFactory(DBPlatform platform, CommentConfig config) {
        this.platform = platform;
        this.config = config;
    }
    
    public void addUserFile(UserFile userFile) 
            throws WdkModelException, UserFileUploadException {
        String filePath = config.getUserFileUploadDir();
        String fileName = userFile.getFileName();
        File fileOnDisk = null;
        
        try {
            if (!fileName.equals("")) {
                logger.debug("File save path:" +filePath);
                fileOnDisk = new File(filePath, fileName);
  
                int rev = 0;
                String[] nameParts = parseFilename(fileName);
                while (fileOnDisk.exists()) {
                  rev++;
                  String newName = nameParts[0] + ".rev" + rev +
                      ((nameParts[1] != null) ? "." + nameParts[1] : "");
                  fileOnDisk = new File(filePath, newName);
                  userFile.setFileName(newName);
                }
  
  
                FileOutputStream fileOutStream = new FileOutputStream(fileOnDisk);
                fileOutStream.write(userFile.getFileData());
                fileOutStream.flush();
                fileOutStream.close();
    
                userFile.setChecksum(md5sum(fileOnDisk));
                logger.debug("MD5 " + userFile.getChecksum());
                
                userFile.setFormat(getFormat(fileOnDisk));
                
                insertUserFileMetaData(userFile);
            }
        } catch (IOException ioe) {
            String msg = "Could not write '" + fileName + "' to '" + filePath + "'";
            logger.warn(msg);
            throw new UserFileUploadException(msg + "\n" + ioe);
        } catch (Exception e) {
            logger.warn(e);
            logger.warn("Deleting " + fileOnDisk.getPath());
            if (fileOnDisk.exists() && ! fileOnDisk.delete())
                logger.warn("\nUnable to delete " + fileOnDisk.getPath() +
                    ". This file may not be correctly recorded in the database.");
            throw new UserFileUploadException(e);
        }

    }

    public void insertUserFileMetaData(UserFile userFile) throws WdkModelException {
        String userFileSchema = config.getUserFileSchema();

        PreparedStatement ps = null;
        try {
            int userFileId = platform.getNextId(userFileSchema, "UserFile");

            ps = SqlUtils.getPreparedStatement(platform.getDataSource(),
                    "INSERT INTO " + userFileSchema + "userfile ("
                            + "userFileId, filename, "
                            + "checksum, uploadTime, "
                            + "ownerUserId, title, notes, "
                            + "projectName, projectVersion, "
                            + "email, format)"
                            + " VALUES (?,?,?,?,?,?,?,?,?,?,?)");
            long currentMillis = System.currentTimeMillis();
            
            ps.setInt(1, userFileId);
            ps.setString(2, userFile.getFileName());
            ps.setString(3, userFile.getChecksum());
            ps.setTimestamp(4, new Timestamp(currentMillis));
            ps.setString(5,  userFile.getUserUID());
            ps.setString(6,  userFile.getTitle());
            ps.setString(7,  userFile.getNotes());
            ps.setString(8,  userFile.getProjectName());
            ps.setString(9,  userFile.getProjectVersion());
            ps.setString(10, userFile.getEmail());
            ps.setString(11, userFile.getFormat());

            int result = ps.executeUpdate();
            
            userFile.setUserFileId(userFileId);
            
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new WdkModelException(ex);
        } finally {
            try {
                SqlUtils.closeStatement(ps);
            } catch (SQLException ex) {
                throw new WdkModelException(ex);
            }
        }
    }
    
    public UserFile getUserFile(int commentId) throws WdkModelException {
        return null;
    }



    public CommentConfig getCommentConfig() {
        return config;
    }

    private String md5sum (File fileOnDisk) throws IOException, NoSuchAlgorithmException {
      String result = "";
      try {
        InputStream fis =  new FileInputStream(fileOnDisk);
        
        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;
        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);
        fis.close();
        byte[] b = complete.digest();
        for (int i=0; i < b.length; i++) {
            result +=
            Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
        }
      } catch (IOException ioe) {
        System.err.println("failed checksum " + ioe);
        throw ioe;
      }
      return result;
    }
    
    private String getFormat(File fileOnDisk) throws IOException {

      ProcessBuilder proc = new ProcessBuilder("/usr/bin/file", "-zb", fileOnDisk.getAbsolutePath());
      Process p = proc.start();
      InputStream is = p.getInputStream();
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      String fmt = br.readLine();
      return fmt;

    }
    
    private String[] parseFilename(String fileName) {
        File tmpFile = new File(fileName);
        String[] nameAr = new String[] {fileName, null};
        tmpFile.getName();
        int idx = tmpFile.getName().lastIndexOf('.');
        if (idx > 0 && idx <= tmpFile.getName().length() - 2 ) {
            nameAr[0] = tmpFile.getName().substring(0, idx);
            nameAr[1] = tmpFile.getName().substring(idx + 1);
        }
        return nameAr;
    }
}
