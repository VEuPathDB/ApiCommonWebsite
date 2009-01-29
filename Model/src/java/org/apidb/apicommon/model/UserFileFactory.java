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
import java.security.NoSuchAlgorithmException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import java.io.*;
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

        try {
          if (!fileName.equals("")) {
              System.out.println("Server path:" +filePath);
              File fileOnDisk = new File(filePath, fileName);

              int rev = 0;
              String[] nameParts = parseFilename(fileName);
              while (fileOnDisk.exists()) {
                rev++;
                String newName = nameParts[0] + ".rev" + rev +
                    ((nameParts[1] != null) ? "." + nameParts[1] : "");
                fileOnDisk = new File(filePath, newName);
              }

              userFile.setFileName(fileName);

              FileOutputStream fileOutStream = new FileOutputStream(fileOnDisk);
              fileOutStream.write(userFile.getFileData());
              fileOutStream.flush();
              fileOutStream.close();
  
              userFile.setChecksum(md5sum(fileOnDisk));
              System.out.println("MD5 " + userFile.getChecksum());    
          }
        } catch (Exception e) {
            System.err.println("error " + e);
            throw new UserFileUploadException(e);
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
