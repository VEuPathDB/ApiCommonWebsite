package org.apidb.apicommon.controller;

import java.util.Map;

import org.apidb.apicommon.model.comment.CommentFactory;
import org.apidb.apicommon.model.comment.pojo.Author;
import org.gusdb.fgputil.accountdb.UserProfile;
import org.gusdb.fgputil.events.Event;
import org.gusdb.fgputil.events.Events;
import org.gusdb.wdk.events.UserProfileUpdateEvent;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;

public class ApiSiteSetup {

  /**
   * Initialize any parts of the ApiCommon web application not handled by normal
   * WDK initialization.
   *
   * @param wdkModel initialized WDK model
   */
  public static void initialize(WdkModel wdkModel) {
    // add user profile update event listener
    Events.subscribe(ApiSiteSetup::userProfileUpdateListener,
        UserProfileUpdateEvent.class);
  }

  /**
   * This code replaces a long-standing DB trigger that was used to update
   * comment search text if the user changed their profile information.  It must
   * collect comment text for any comments owned by the revised user and update
   * their cached search text in the DB.
   */
  private static void userProfileUpdateListener(Event event)
      throws WdkModelException {
    UserProfileUpdateEvent updateEvent = (UserProfileUpdateEvent) event;

    // check to see if any of the property text fields changed
    Map<String, String> userProps = updateEvent.getNewProfile().getProperties();
    Map<String, String> oldProfileProps = updateEvent.getOldProfile()
        .getProperties();
    boolean commentSearchTextUpdateRequired = false;

    for (String key : oldProfileProps.keySet()) {
      if (!oldProfileProps.get(key).equals(userProps.get(key))) {
        commentSearchTextUpdateRequired = true;
      }
    }

    // if none changed, no update needed
    if (!commentSearchTextUpdateRequired)
      return;

    // need to write updated text to comment search field
    CommentFactory commentFactory = CommentFactoryManager.getCommentFactory(
        updateEvent.getWdkModel().getProjectId());
    UserProfile profile = updateEvent.getNewProfile();

    commentFactory.updateAuthor(new Author()
        .setFirstName(profile.getProperties().get("firstName"))
        .setLastName(profile.getProperties().get("lastName"))
        .setOrganization(profile.getProperties().get("organization"))
        .setUserId(profile.getUserId()));
  }
}
