package org.apidb.apicommon.model.comment.repo;

public interface Table {
  String COMMENT_TO_EXT_DB    = "COMMENT_EXTERNAL_DATABASE";
  String COMMENT_TO_REFERENCE = "COMMENTREFERENCE";
  String COMMENT_TO_STABLE_ID = "COMMENTSTABLEID";
  String COMMENT_TO_SEQUENCE  = "COMMENTSEQUENCE";
  String COMMENT_TO_CATEGORY  = "COMMENTTARGETCATEGORY";
  String COMMENT_TO_LOCATION  = "LOCATIONS";
  String COMMENT_TO_FILE      = "COMMENTFILE";

  String COMMENT_USERS = "COMMENT_USERS";
  String COMMENTS      = "COMMENTS";
  String EXTERNAL_DBS  = "EXTERNAL_DATABASES";
  String CATEGORIES    = "TARGETCATEGORY";

  // AI-assisted gene-publication summary sidecar tables (see
  // Service/CLAUDE-ai-user-comments.md). Live in the same comment schema as
  // COMMENTS so comment_ai_provenance.comment_id can FK to comments.comment_id.
  String COMMENT_AI_RUN        = "COMMENT_AI_RUN";
  String COMMENT_AI_PROVENANCE = "COMMENT_AI_PROVENANCE";
}
