package org.apidb.apicommon.model.comment.repo;

interface Column {
  interface Comment {
    String
      DATE              = "comment_date",
      ID                = "comment_id",
      TARGET_ID         = "comment_target_id",
      CONCEPTUAL        = "conceptual",
      CONTENT           = "content",
      HEADLINE          = "headline",
      LOCATION          = "location_string",
      ORGANISM          = "organism",
      PROJECT_NAME      = "project_name",
      PROJECT_VERSION   = "project_version",
      REVIEW_STATUS     = "review_status_id",
      STABLE_ID         = "stable_id",
      USER_ID           = "user_id",
      VISIBLE           = "is_visible";
  }

  interface CommentSequence {
    String SEQUENCE = "sequence",
      COMMENT_ID = "comment_id";
  }

  interface CommentStableId {
    String STABLE_ID = "stable_id",
      COMMENT_ID = "comment_id";
  }

  interface CommentReference {
    String DATABASE = "database_name",
      SOURCE_ID = "source_id",
      COMMENT_ID = "comment_id";
  }

  interface CommentFile {
    String ID = "file_id",
      NAME = "name",
      NOTES = "notes",
      COMMENT_ID = "comment_id";
  }

  interface CommentUser {
    String ID = "user_id",
      FIRST_NAME   = "first_name",
      LAST_NAME    = "last_name",
      ORGANIZATION = "organization";
  }


  interface ExternalDatabase {
    String ID = "external_database_id",
      NAME = "external_database_name",
      VERSION = "external_database_version";
  }

  interface Location {
    String COORD_TYPE = "coordinate_type",
      START = "location_start",
      END = "location_end",
      REVERSED = "is_reverse",
      COMMENT_ID = "comment_id";
  }

  interface TargetCategory {
    String ID = "target_category_id",
      NAME = "category",
      TARGET_ID = "comment_target_id";
  }

  interface CommentExternalDb {
    String COMMENT_ID = "comment_id",
      DB_ID = "external_database_id";
  }

  interface CommentCategory {
    String COMMENT_ID = "comment_id",
      CATEGORY_ID = "target_category_id";
  }
}
