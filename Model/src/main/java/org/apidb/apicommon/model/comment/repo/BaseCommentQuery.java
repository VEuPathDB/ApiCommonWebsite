package org.apidb.apicommon.model.comment.repo;

import org.apidb.apicommon.model.comment.ReferenceType;
import org.apidb.apicommon.model.comment.ReviewStatus;
import org.apidb.apicommon.model.comment.pojo.*;
import org.gusdb.fgputil.Tuples;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

abstract class BaseCommentQuery<T> extends ValueQuery<T> {
  protected BaseCommentQuery(String schema) {
    super(schema);
  }

  protected static Optional<String> rs2Sequence(ResultSet rs) throws SQLException {
    return Optional.ofNullable(rs.getString(Column.CommentSequence.SEQUENCE));
  }

  protected static Optional<Locations> rs2Location(ResultSet rs) throws SQLException {
    final String coordType = rs.getString(Column.Location.COORD_TYPE);
    if(coordType == null)
      return Optional.empty();

    return Optional.of(new Locations().setCoordinateType(coordType)
        .setReversed(rs.getBoolean(Column.Location.REVERSED)));
  }

  protected static Optional<LocationRange> rs2LocationRange(ResultSet rs) throws SQLException {
    final long start = rs.getLong(Column.Location.START);

    if(rs.wasNull())
      return Optional.empty();

    final long end = rs.getLong(Column.Location.END);

    if(rs.wasNull())
      return Optional.empty();

    return Optional.of(new LocationRange(start, end));
  }

  protected static Optional<String> rs2Related(ResultSet rs) throws SQLException {
    return Optional.ofNullable(rs.getString(Column.CommentStableId.STABLE_ID));
  }

  /**
   * Convert result row to external database (if present)
   *
   * @param rs Database row cursor
   *
   * @return An ExternalDatabase instance if the given row contains external db
   *         details.  Otherwise none.
   */
  protected static Optional<ExternalDatabase> rs2ExternalDb(ResultSet rs)
      throws SQLException {

    final String name = rs.getString(Column.ExternalDatabase.NAME);
    final String version = rs.getString(Column.ExternalDatabase.VERSION);

    if(name == null || version == null)
      return Optional.empty();

    return Optional.of(new ExternalDatabase().setName(name).setVersion(version));
  }

  /**
   * Convert result row to reference (if present)
   *
   * @param rs Database row cursor
   *
   * @return A tuple containing a reference type and ID if the given row
   *         contains a reference.  Otherwise none.
   */
  protected static Optional<Tuples.TwoTuple<ReferenceType, String>> rs2Reference(
      ResultSet rs) throws SQLException {

    final String value = rs.getString(Column.CommentReference.SOURCE_ID);

    if(value == null)
      return Optional.empty();

    return ReferenceType.fromDbName(rs.getString(Column.CommentReference.DATABASE))
        .map(t -> new Tuples.TwoTuple<>(t, value));
  }

  protected static void appendReference(Comment com,
      Tuples.TwoTuple<ReferenceType, String> tup) {

    switch (tup.getFirst()) {
      case DIGITAL_OBJECT_ID:
        com.addDigitalObjectId(tup.getSecond());
        break;
      case ACCESSION:
        com.addGenBankAccession(tup.getSecond());
        break;
      case AUTHOR:
        com.addAdditionalAuthor(tup.getSecond());
        break;
      case PUB_MED:
        com.addPubMedId(tup.getSecond());
        break;
    }
  }

  /**
   * ResultSet to User Comment
   *
   * Parses columns out of a result set row to create an instance of Comment.
   *
   * @param rs ResultSet handle on a DB row.
   *
   * @return Newly created comment from the given row.
   */
  protected static Comment rs2Comment(final ResultSet rs) throws SQLException {
    final Comment out = new Comment(rs.getLong(Column.Comment.ID), rs.getLong(
        Column.Comment.USER_ID))
        .setCommentDate(rs.getDate(Column.Comment.DATE))
        .setConceptual(rs.getInt(Column.Comment.CONCEPTUAL) == 1)
        .setProject(rs2Project(rs))
        .setHeadline(rs.getString(Column.Comment.HEADLINE))
        .setReviewStatus(rs2ReviewStatus(rs))
        .setContent(rs.getString(Column.Comment.CONTENT))
        .setOrganism(rs.getString(Column.Comment.ORGANISM));

    out.getTarget()
        .setId(rs.getString(Column.Comment.STABLE_ID))
        .setType(rs.getString(Column.Comment.TARGET_ID));

    return out;
  }

  /**
   * ResultSet to Project details
   *
   * Parses relevant columns out of a result set to create an instance of
   * Project.
   *
   * @param rs ResultSet cursor to DB row.
   *
   * @return Newly created Project instance from the given row.
   */
  private static Project rs2Project(final ResultSet rs) throws SQLException {
    return new Project(rs.getString(Column.Comment.PROJECT_NAME),
        rs.getString(Column.Comment.PROJECT_VERSION));
  }

  /**
   * ResultSet to Comment Review Status
   *
   * Parses relevant columns out of a result set to create an instance of
   * ReviewStatus.
   *
   * @param rs ResultSet cursor to DB row.
   *
   * @return Parsed ReviewStatus value
   */
  private static ReviewStatus rs2ReviewStatus(final ResultSet rs) throws SQLException {
    return ReviewStatus.fromString(rs.getString(
        Column.Comment.REVIEW_STATUS));
  }
}
