package org.apidb.apicommon.model.comment.repo;

import org.apidb.apicommon.model.comment.pojo.Category;
import org.apidb.apicommon.model.comment.repo.Column.TargetCategory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;

import static java.util.Objects.isNull;

public class GetCategoriesQuery extends ValueQuery<Collection<Category>> {

  private static final String
    SQL = "SELECT\n" +
      "  " + TargetCategory.ID + ",\n" +
      "  " + TargetCategory.NAME + "\n" +
      "FROM\n  %s." + Table.CATEGORIES,
    WHERE = "\nWHERE " + TargetCategory.TARGET_ID + " = ?";

  private static final Integer[] TYPES = { Types.VARCHAR };
  private String type;

  public GetCategoriesQuery(String schema) {
    super(schema);
  }

  public GetCategoriesQuery filterByType(final String type) {
    this.type = type;
    return this;
  }

  @Override
  protected Collection<Category> parseResults(ResultSet rs)
  throws SQLException {
    final Collection<Category> out = new ArrayList<>();
    while (rs.next())
      out.add(rs2Category(rs));
    return out;
  }

  @Override
  protected Object[] getParams() {
    return hasFilter() ? new Object[] { type } : new Object[0];
  }

  @Override
  protected Integer[] getTypes() {
    return hasFilter() ? TYPES : new Integer[0];
  }

  @Override
  protected String getQuery() {
    return hasFilter() ? SQL + WHERE : SQL;
  }

  private boolean hasFilter() {
    return !isNull(type);
  }

  static Category rs2Category(final ResultSet rs) throws SQLException {
    return new Category(
      rs.getInt(TargetCategory.ID),
      rs.getString(TargetCategory.NAME)
    );
  }
}
