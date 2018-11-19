package org.apidb.apicommon.model.userfile;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.apidb.apicommon.model.comment.CommentConfig;
import org.apidb.apicommon.model.comment.CommentConfigParser;
import org.apidb.apicommon.model.comment.CommentModelException;
import org.gusdb.fgputil.db.SqlUtils;
import org.gusdb.fgputil.db.platform.DBPlatform;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.runtime.Manageable;
import org.gusdb.wdk.model.WdkModelException;

public class UserFileFactory implements Manageable<UserFileFactory> {

  private static Logger LOG = Logger.getLogger(UserFileFactory.class);

  private DatabaseInstance _database;
  private DBPlatform _platform;
  private CommentConfig _config;
  private String _projectId;

  @Override
  public UserFileFactory getInstance(String projectId, String gusHome) throws WdkModelException {
    // parse and load the configuration
    CommentConfigParser parser = new CommentConfigParser(gusHome);
    try {
      CommentConfig config = parser.parseConfig(projectId);

      // create a platform object
      DatabaseInstance database = new DatabaseInstance(config, "Comment");

      // create a factory instance
      UserFileFactory factory = new UserFileFactory();
      factory.initialize(database, config, projectId);
      return factory;
    }
    catch (CommentModelException ex) {
      throw new WdkModelException(ex);
    }
  }

  private void initialize(DatabaseInstance database, CommentConfig config, String projectId) {
    // this.dataSource = database.getDataSource();
    this._database = database;
    this._platform = database.getPlatform();
    this._config = config;
    this._projectId = projectId;
  }

  public InputStream getUserFile(String fname) throws WdkModelException {
    File out = new File(_config.getUserFileUploadDir() + "/" + _projectId +
        "/" + fname);

    if(!out.exists())
      throw new WdkModelException(String.format("user file %s does not exist",
          fname));

    if(!out.canRead())
      throw new WdkModelException(String.format("user file %s is not readable",
          fname));

    try {
      return new FileInputStream(out);
    } catch (FileNotFoundException e) {
      throw new WdkModelException(e);
    }
  }

  public void addUserFile(UserFile userFile) throws UserFileUploadException {
    File filePath = new File(_config.getUserFileUploadDir() + "/" + _projectId);
    String fileName = userFile.getFileName();
    File fileOnDisk = null;

    if (!filePath.exists())
      filePath.mkdirs();

    try {
      if (!fileName.equals("")) {
        LOG.debug("File save path:" + filePath.toString());
        fileOnDisk = new File(filePath, fileName);

        int ver = 0;
        String[] nameParts = parseFilename(fileName);
        // add version to file name if already exists.
        // also checking database to avoid conflict when file is added
        // to another server but not yet synched to current server
        while (fileOnDisk.exists() || nameExistsInDb(fileOnDisk.getName())) {
          ver++;
          String newName = nameParts[0] + ".copy_" + ver + ((nameParts[1] != null) ? "." + nameParts[1] : "");
          fileOnDisk = new File(filePath, newName);
          userFile.setFileName(newName);
        }

        FileOutputStream fileOutStream = new FileOutputStream(fileOnDisk);
        fileOutStream.write(userFile.getFileData());
        fileOutStream.flush();
        fileOutStream.close();

        userFile.setChecksum(md5sum(fileOnDisk));
        LOG.debug("MD5 " + userFile.getChecksum());

        userFile.setFormat(getFormat(fileOnDisk));
        userFile.setFileSize(fileOnDisk.length());

        insertUserFileMetaData(userFile);
      }
      else {
        throw new UserFileUploadException("File name not specified.");
      }
    }
    catch (IOException ioe) {
      String msg = "Could not write '" + fileName + "' to '" + filePath + "'";
      LOG.warn(msg);
      throw new UserFileUploadException(msg + "\n" + ioe);
    }
    catch (Exception e) {
      LOG.warn(e);
      if (fileOnDisk != null) {
        LOG.warn("Deleting " + fileOnDisk.getPath());
        if (fileOnDisk.exists() && !fileOnDisk.delete())
          LOG.warn("\nUnable to delete " + fileOnDisk.getPath() +
              ". This file may not be correctly recorded in the database.");
      }
      throw new UserFileUploadException(e);
    }

  }

  public void insertUserFileMetaData(UserFile userFile) throws WdkModelException {
    String userFileSchema = _config.getUserFileSchema();

    PreparedStatement ps = null;
    try {
      DataSource dataSource = _database.getDataSource();
      long userFileId = _platform.getNextId(dataSource, userFileSchema, "UserFile");

      ps = SqlUtils.getPreparedStatement(dataSource, "INSERT INTO " + userFileSchema + "userfile (" +
          "userFileId, filename, " + "checksum, uploadTime, " + "ownerUserId, title, notes, " +
          "projectName, projectVersion, " + "email, format, filesize)" + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
      long currentMillis = System.currentTimeMillis();

      ps.setLong(1, userFileId);
      ps.setString(2, userFile.getFileName());
      ps.setString(3, userFile.getChecksum());
      ps.setTimestamp(4, new Timestamp(currentMillis));
      ps.setString(5, userFile.getUserUID());
      ps.setString(6, userFile.getTitle());
      ps.setString(7, userFile.getNotes());
      ps.setString(8, userFile.getProjectName());
      ps.setString(9, userFile.getProjectVersion());
      ps.setString(10, userFile.getEmail());
      ps.setString(11, userFile.getFormat());
      ps.setLong(12, userFile.getFileSize());

      ps.executeUpdate();

      userFile.setUserFileId(userFileId);

    }
    catch (SQLException ex) {
      ex.printStackTrace();
      throw new WdkModelException(ex);
    }
    finally {
      SqlUtils.closeStatement(ps);
    }
  }

  public CommentConfig getCommentConfig() {
    return _config;
  }

  private String md5sum(File fileOnDisk) throws IOException, NoSuchAlgorithmException {
    String result = "";
    try {
      InputStream fis = new FileInputStream(fileOnDisk);

      byte[] buffer = new byte[1024];
      MessageDigest complete = MessageDigest.getInstance("MD5");
      int numRead;
      do {
        numRead = fis.read(buffer);
        if (numRead > 0) {
          complete.update(buffer, 0, numRead);
        }
      }
      while (numRead != -1);
      fis.close();
      byte[] b = complete.digest();
      for (int i = 0; i < b.length; i++) {
        result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
      }
    }
    catch (IOException ioe) {
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
    return fmt.substring(0, Math.min(fmt.length(), 255));

  }

  private String[] parseFilename(String fileName) {
    File tmpFile = new File(fileName);
    String[] nameAr = new String[] { fileName, null };
    tmpFile.getName();
    int idx = tmpFile.getName().lastIndexOf('.');
    if (idx > 0 && idx <= tmpFile.getName().length() - 2) {
      nameAr[0] = tmpFile.getName().substring(0, idx);
      nameAr[1] = tmpFile.getName().substring(idx + 1);
    }
    return nameAr;
  }

  private boolean nameExistsInDb(String filename) throws WdkModelException {
    String userFileSchema = _config.getUserFileSchema();

    boolean exists = true;

    String query = "select count(*) count from " + userFileSchema + "userfile " + "where filename = ?";
    ResultSet rs = null;
    PreparedStatement ps = null;
    try {
      ps = SqlUtils.getPreparedStatement(_database.getDataSource(), query);
      ps.setString(1, filename);
      rs = ps.executeQuery();
      if (!rs.next()) {
        throw new WdkModelException("Unable to query for filename " + filename);
      }
      exists = (rs.getInt("count") != 0);
    }
    catch (SQLException ex) {
      ex.printStackTrace();
      throw new WdkModelException(ex);
    }
    finally {
      SqlUtils.closeResultSetAndStatement(rs, ps);
    }
    return exists;
  }
}
