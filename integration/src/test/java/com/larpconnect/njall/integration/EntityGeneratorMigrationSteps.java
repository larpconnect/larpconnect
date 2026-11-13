package com.larpconnect.njall.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Splitter;
import io.cucumber.java.en.Then;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

/** Cucumber step definitions verifying V13 entity generator and collection owner schema state. */
public final class EntityGeneratorMigrationSteps {

  private static final String COLUMN_EXISTS_SQL =
      "SELECT 1 FROM information_schema.columns "
          + "WHERE table_schema = ? AND table_name = ? AND column_name = ?";

  private static final String COLUMN_NULLABILITY_SQL =
      "SELECT is_nullable FROM information_schema.columns "
          + "WHERE table_schema = ? AND table_name = ? AND column_name = ?";

  private static final String CONSTRAINT_EXISTS_SQL =
      "SELECT 1 FROM pg_constraint c "
          + "JOIN pg_class t ON c.conrelid = t.oid "
          + "JOIN pg_namespace n ON t.relnamespace = n.oid "
          + "WHERE n.nspname = ? AND t.relname = ? AND c.conname = ? AND c.contype = ?";

  private static final String INDEX_EXISTS_SQL =
      "SELECT 1 FROM pg_indexes WHERE schemaname = ? AND tablename = ? AND indexname = ?";

  @Then("table {string} has non-null column {string} and no column {string}")
  public void tableHasNonNullColumnAndNoColumn(
      String fullTable, String expectedColumn, String absentColumn) throws Exception {
    var parts = splitSchemaTable(fullTable);
    try (var conn = openNjallConnection()) {
      assertThat(isColumnNotNull(conn, parts.getFirst(), parts.get(1), expectedColumn)).isTrue();
      assertThat(columnExists(conn, parts.getFirst(), parts.get(1), absentColumn)).isFalse();
    }
  }

  @Then(
      "table {string} has foreign key constraint {string} and index {string} and no constraint"
          + " {string} or index {string}")
  public void tableHasForeignKeyConstraintAndIndexWithoutLegacy(
      String fullTable,
      String expectedConstraint,
      String expectedIndex,
      String absentConstraint,
      String absentIndex)
      throws Exception {
    var parts = splitSchemaTable(fullTable);
    var schema = parts.getFirst();
    var table = parts.get(1);
    try (var conn = openNjallConnection()) {
      assertThat(constraintExists(conn, schema, table, expectedConstraint, "f")).isTrue();
      assertThat(constraintExists(conn, schema, table, absentConstraint, "f")).isFalse();
      assertThat(indexExists(conn, schema, table, expectedIndex)).isTrue();
      assertThat(indexExists(conn, schema, table, absentIndex)).isFalse();
    }
  }

  @Then(
      "table {string} retains column {string}, constraint {string}, constraint {string}, and index"
          + " {string}")
  public void tableRetainsColumnConstraintsAndIndex(
      String fullTable,
      String column,
      String fkConstraint,
      String uniqueConstraint,
      String indexName)
      throws Exception {
    var parts = splitSchemaTable(fullTable);
    var schema = parts.getFirst();
    var table = parts.get(1);
    try (var conn = openNjallConnection()) {
      assertThat(columnExists(conn, schema, table, column)).isTrue();
      assertThat(constraintExists(conn, schema, table, fkConstraint, "f")).isTrue();
      assertThat(constraintExists(conn, schema, table, uniqueConstraint, "u")).isTrue();
      assertThat(indexExists(conn, schema, table, indexName)).isTrue();
    }
  }

  private static List<String> splitSchemaTable(String fullTable) {
    return Splitter.on('.').splitToList(fullTable);
  }

  private static Connection openNjallConnection() throws SQLException {
    return DriverManager.getConnection(
        DatabaseMigrationSteps.getJdbcUrl(),
        "njall",
        DatabaseMigrationSteps.getPasswordFor("njall"));
  }

  private static boolean columnExists(Connection conn, String schema, String table, String column)
      throws SQLException {
    try (var stmt = conn.prepareStatement(COLUMN_EXISTS_SQL)) {
      stmt.setString(1, schema);
      stmt.setString(2, table);
      stmt.setString(3, column);
      try (var rs = stmt.executeQuery()) {
        return rs.next();
      }
    }
  }

  private static boolean isColumnNotNull(
      Connection conn, String schema, String table, String column) throws SQLException {
    try (var stmt = conn.prepareStatement(COLUMN_NULLABILITY_SQL)) {
      stmt.setString(1, schema);
      stmt.setString(2, table);
      stmt.setString(3, column);
      try (var rs = stmt.executeQuery()) {
        return rs.next() && "NO".equals(rs.getString("is_nullable"));
      }
    }
  }

  private static boolean constraintExists(
      Connection conn, String schema, String table, String constraintName, String contype)
      throws SQLException {
    try (var stmt = conn.prepareStatement(CONSTRAINT_EXISTS_SQL)) {
      stmt.setString(1, schema);
      stmt.setString(2, table);
      stmt.setString(3, constraintName);
      stmt.setString(4, contype);
      try (var rs = stmt.executeQuery()) {
        return rs.next();
      }
    }
  }

  private static boolean indexExists(Connection conn, String schema, String table, String indexName)
      throws SQLException {
    try (var stmt = conn.prepareStatement(INDEX_EXISTS_SQL)) {
      stmt.setString(1, schema);
      stmt.setString(2, table);
      stmt.setString(3, indexName);
      try (var rs = stmt.executeQuery()) {
        return rs.next();
      }
    }
  }
}
