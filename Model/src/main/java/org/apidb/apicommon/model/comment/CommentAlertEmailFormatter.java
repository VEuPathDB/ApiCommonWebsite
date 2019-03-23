package org.apidb.apicommon.model.comment;

import org.apidb.apicommon.model.comment.pojo.CommentRequest;
import org.apidb.apicommon.model.comment.pojo.ExternalDatabase;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.user.User;

import java.util.Collection;
import java.util.stream.Collectors;

public class CommentAlertEmailFormatter {
  private static final String
    FIELD_COMMENT_ID          = "Comment Id",
    FIELD_HEADLINE            = "Headline",
    FIELD_TARGET_TYPE         = "Target",
    FIELD_TARGET_ID           = "Source Id",
    FIELD_CONTENT             = "Comment",
    FIELD_PUB_MED_IDS         = "PMID",
    FIELD_DIGITIAL_OBJECT_IDS = "DOI(s)",
    FIELD_RELATED_RECORDS     = "Related Genes",
    FIELD_GEN_BANK_ACCESSIONS = "Accession",
    FIELD_AUTHOR_EMAIL        = "Email",
    FIELD_ORGANISM            = "Organism",
    FIELD_EXTERNAL_DB_NAME    = "DB Name",
    FIELD_EXTERNAL_DB_VERSION = "DB Version",
    FIELD_LINK_TO_COMMENT     = "Comment Link",
    FIELD_REDMINE_PROJECT_ID  = "Project",
    FIELD_TRACKER             = "Tracker",
    FIELD_ASSIGNEE            = "Assignee",
    FIELD_EUPATH_DB_TEAM      = "EuPathDB Team",
    FIELD_PROJECT_ID          = "Component";

  private static final String DIVIDER = "-------------------------------------------------------";

  private static final String LINE_SEPARATOR = "<br/>\n";

  private static final String FIELD_SEPARATOR = ": ";

  private static final String LIST_SEPARATOR = ", ";

  private static final String
    REDMINE_PROJECT_ID   = "uiresulvb",
    REDMINE_TRACKER      = "Communication",
    REDMINE_ASSIGNEE     = "annotator",
    EUPATH_DB_TEAM       = "Outreach";

  public String makeSelfAlertBody(WdkModel wdk, User user,
      CommentRequest comment, long commentId, String url) {
    final StringBuilder body = new StringBuilder();
    final String organism = comment.getOrganism();
    final ExternalDatabase exDb = comment.getExternalDatabase();

    body.append(formatLine(getThanks(wdk.getProjectId(), organism)))
        .append(formatLine(DIVIDER))
        .append(formatField(FIELD_COMMENT_ID, commentId))
        .append(formatField(FIELD_HEADLINE, comment.getHeadline()))
        .append(formatField(FIELD_TARGET_TYPE, comment.getTarget().getType()))
        .append(formatField(FIELD_TARGET_ID, comment.getTarget().getId()))
        .append(formatField(FIELD_CONTENT, comment.getContent()))
        .append(formatField(FIELD_PUB_MED_IDS, comment.getPubMedIds()))
        .append(formatField(FIELD_DIGITIAL_OBJECT_IDS, comment.getDigitalObjectIds()))
        .append(formatField(FIELD_RELATED_RECORDS, comment.getRelatedStableIds()))
        .append(formatField(FIELD_GEN_BANK_ACCESSIONS, comment.getGenBankAccessions()))
        .append(formatField(FIELD_AUTHOR_EMAIL, user.getEmail()));

    if(organism != null)
      body.append(formatField(FIELD_ORGANISM, organism));

    if(exDb != null) {
      body.append(formatField(FIELD_EXTERNAL_DB_NAME, exDb.getName()));
      body.append(formatField(FIELD_EXTERNAL_DB_VERSION, exDb.getVersion()));
    }

    body.append(formatField(
        FIELD_LINK_TO_COMMENT,
        "<a href=\"" + url + "\">" + url + "</a>",
        false
    ));

    return body.append(formatLine(DIVIDER)).toString();
  }

  public String makeRedmineAlertBody(WdkModel wdk, User user,
      CommentRequest comment, long commentId, String url) {
    return new StringBuilder(makeSelfAlertBody(wdk, user, comment, commentId, url))
        .append(formatField(FIELD_REDMINE_PROJECT_ID, REDMINE_PROJECT_ID))
        .append(formatField(FIELD_TRACKER, REDMINE_TRACKER))
        .append(formatField(FIELD_ASSIGNEE, REDMINE_ASSIGNEE))
        .append(formatField(FIELD_EUPATH_DB_TEAM, EUPATH_DB_TEAM))
        .append(formatField(FIELD_PROJECT_ID, wdk.getProjectId()))
        .toString();
  }

  public String makeSubject(String projectId, CommentRequest com) {
    return String.format("%s %s %s", projectId, com.getTarget().getType(),
        com.getTarget().getId());
  }

  private static String formatLine(String line) {
    return line + LINE_SEPARATOR;
  }

  private static String formatField(String field, Object value) {
    return formatField(field, value, true);
  }

  private static String formatField(String field, Object value, boolean escape) {
    String val = (value instanceof Collection)
      ? formatList((Collection) value)
      : String.valueOf(value);
    if (escape)
      val = FormatUtil.escapeHtml(val);
    return formatLine(field + FIELD_SEPARATOR + val);
  }

  private static String formatList(Collection<?> col) {
    return col.stream()
      .map(String::valueOf)
      .collect(Collectors.joining(LIST_SEPARATOR));
  }

  private static String getThanks(String project, String organism) {
    return "TriTrypDB".equals(project)
      || "Plasmodium falciparum".equals(organism)
      || "Cryptosporidium parvum".equals(organism)
      ? "Thank you! Your comment will be reviewed by a curator shortly."
      : "Thanks for your comment!";

  }
}
