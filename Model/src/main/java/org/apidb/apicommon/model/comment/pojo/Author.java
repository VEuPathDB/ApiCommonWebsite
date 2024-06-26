package org.apidb.apicommon.model.comment.pojo;

import org.gusdb.wdk.model.user.User;

public class Author {

  private String _firstName;

  private String _lastName;

  private String _organization;

  private long _userId;

  public String getFirstName() {
    return _firstName;
  }

  public Author setFirstName(String firstName) {
    _firstName = firstName;
    return this;
  }

  public String getLastName() {
    return _lastName;
  }

  public Author setLastName(String lastName) {
    _lastName = lastName;
    return this;
  }

  public String getOrganization() {
    return _organization;
  }

  public Author setOrganization(String organization) {
    _organization = organization;
    return this;
  }

  public long getUserId() {
    return _userId;
  }

  public Author setUserId(long userId) {
    this._userId = userId;
    return this;
  }

  public static Author fromUser(final User user) {
    return new Author()
        .setUserId(user.getUserId())
        .setFirstName(user.getFirstName())
        .setLastName(user.getLastName())
        .setOrganization(user.getOrganization());
  }
}
