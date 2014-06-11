package org.apidb.apicommon.model;

import java.util.HashMap;
import java.util.Map;

import org.apidb.apicommon.model.comment.CommentFactory;
import org.apidb.apicommon.model.comment.CommentModelException;
import org.apidb.apicommon.model.userfile.UserFileFactory;
import org.gusdb.fgputil.runtime.GusHome;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;

public class InstanceManager {

  private static final Map<String, WdkModel> wdkModels = new HashMap<>();
  private static final Map<String, CommentFactory> commentFactories = new HashMap<>();
  private static final Map<String, UserFileFactory> userFileFactories = new HashMap<>();

  public static WdkModel getWdkModel(String projectId) throws WdkModelException {
    projectId = projectId.intern();
    synchronized (projectId) {
      WdkModel wdkModel = wdkModels.get(projectId);
      if (wdkModel == null) {
        String gusHome = GusHome.getGusHome();
        wdkModel = WdkModel.construct(projectId, gusHome);
        wdkModels.put(projectId, wdkModel);
      }
      return wdkModel;
    }
  }

  public static CommentFactory getCommentFactory(String projectId) throws WdkModelException,
      CommentModelException {
    projectId = projectId.intern();
    synchronized (projectId) {
      CommentFactory commentFactory = commentFactories.get(projectId);
      if (commentFactory == null) {
        String gusHome = GusHome.getGusHome();
        commentFactory = CommentFactory.getInstance(gusHome, projectId);
        commentFactories.put(projectId, commentFactory);
      }
      return commentFactory;
    }
  }

  public static UserFileFactory getUserFileFactory(String projectId) throws WdkModelException,
      CommentModelException {
    projectId = projectId.intern();
    synchronized (projectId) {
      UserFileFactory userFileFactory = userFileFactories.get(projectId);
      if (userFileFactory == null) {
        String gusHome = GusHome.getGusHome();
        userFileFactory = UserFileFactory.getInstance(gusHome, projectId);
        userFileFactories.put(projectId, userFileFactory);
      }
      return userFileFactory;
    }
  }
}
