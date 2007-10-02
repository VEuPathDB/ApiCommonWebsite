package org.apidb.apicommon.model;

import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.user.User;
import org.gusdb.wdk.model.user.UserFactory;

public class CommentComposer {

    public static void fillUserInfo(Comment[] comments, WdkModel wdkModel)
            throws WdkUserException {
        // get the user factory
        UserFactory factory = wdkModel.getUserFactory();

        // for each comment, fill in the userName and organization info
        for (Comment comment : comments) {
            try {
                User user = factory.loadUser(comment.getEmail());
                comment.setUserName(user.getFirstName() + " "
                        + user.getLastName());
                comment.setOrganization(user.getOrganization());
            } catch (WdkUserException ex) {
                // the user cannot be found
                comment.setUserName("[unknown user]");
                comment.setOrganization("[unknown organization]");
            }
        }
    }
}
