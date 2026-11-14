package com.larpconnect.njall.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Immutable;
import io.cucumber.java.en.Then;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Cucumber step definitions verifying V14 systems, campaigns, and games schema state. */
public final class SystemsCampaignsGamesMigrationSteps {

  private static final String ENUM_VALUES_SQL =
      "SELECT e.enumlabel FROM pg_enum e "
          + "JOIN pg_type t ON e.enumtypid = t.oid "
          + "JOIN pg_namespace n ON t.typnamespace = n.oid "
          + "WHERE n.nspname = 'njall_users' AND t.typname = ? "
          + "ORDER BY e.enumsortorder";

  private static final String COLUMN_DETAILS_SQL =
      "SELECT is_nullable, udt_schema, udt_name, column_default "
          + "FROM information_schema.columns "
          + "WHERE table_schema = ? AND table_name = ? AND column_name = ?";

  private static final String CONSTRAINT_EXISTS_SQL =
      "SELECT 1 FROM pg_constraint c "
          + "JOIN pg_class t ON c.conrelid = t.oid "
          + "JOIN pg_namespace n ON t.relnamespace = n.oid "
          + "WHERE n.nspname = ? AND t.relname = ? AND c.conname = ? AND c.contype = ?";

  private static final String INDEX_EXISTS_SQL =
      "SELECT 1 FROM pg_indexes WHERE schemaname = ? AND tablename = ? AND indexname = ?";

  private static final String SET_TENANT_SQL = "SELECT set_config('app.tenant_id', ?, true)";
  private static final String SELECT_SYSTEM_IDS_SQL = "SELECT id FROM njall_users.systems";
  private static final String SELECT_CAMPAIGN_IDS_SQL = "SELECT id FROM njall_users.campaigns";
  private static final String SELECT_GAME_IDS_SQL = "SELECT id FROM njall_users.games";

  private static final String INSERT_STUDIO_SQL =
      "INSERT INTO njall_users.studios (id, name) VALUES (?, ?)";
  private static final String INSERT_ENTITY_SQL =
      "INSERT INTO njall_users.entities (id, tenant_id, entity_type, generator) "
          + "VALUES (?, ?, ?, ?)";
  private static final String INSERT_SYSTEM_SQL =
      "INSERT INTO njall_users.systems (tenant_id, id, name) VALUES (?, ?, ?)";
  private static final String INSERT_CAMPAIGN_SQL =
      "INSERT INTO njall_users.campaigns (tenant_id, id, system_id, title) VALUES (?, ?, ?, ?)";
  private static final String INSERT_EVENT_SQL =
      "INSERT INTO njall_users.events (tenant_id, id, title, event_type) "
          + "VALUES (?, ?, ?, ?::njall_users.tevent_type)";
  private static final String INSERT_GAME_SQL =
      "INSERT INTO njall_users.games (tenant_id, id, system_id, campaign_id) "
          + "VALUES (?, ?, ?, ?)";

  private static final String DELETE_GAMES_SQL =
      "DELETE FROM njall_users.games WHERE tenant_id = ?";
  private static final String DELETE_EVENTS_SQL =
      "DELETE FROM njall_users.events WHERE tenant_id = ?";
  private static final String DELETE_CAMPAIGNS_SQL =
      "DELETE FROM njall_users.campaigns WHERE tenant_id = ?";
  private static final String DELETE_SYSTEMS_SQL =
      "DELETE FROM njall_users.systems WHERE tenant_id = ?";
  private static final String DELETE_ENTITIES_SQL =
      "DELETE FROM njall_users.entities WHERE tenant_id = ?";
  private static final String DELETE_STUDIO_SQL = "DELETE FROM njall_users.studios WHERE id = ?";

  @Immutable
  private record TenantFixture(
      UUID tenantId, UUID actorId, UUID systemId, UUID campaignId, UUID gameId) {}

  @Then(
      "PostgreSQL enum {string} exists with values {string}, {string}, {string}, {string}, and"
          + " {string}")
  public void postgresEnumExistsWithValues(
      String enumName, String v1, String v2, String v3, String v4, String v5) throws Exception {
    var labels = ImmutableList.<String>builder();
    try (var conn = openConnection("njall");
        var stmt = conn.prepareStatement(ENUM_VALUES_SQL)) {
      stmt.setString(1, enumName);
      try (var rs = stmt.executeQuery()) {
        while (rs.next()) {
          labels.add(rs.getString("enumlabel"));
        }
      }
    }
    assertThat(labels.build()).containsExactly(v1, v2, v3, v4, v5);
  }

  @Then("table {string} has non-null column {string} of type {string} with default {string}")
  public void tableHasNonNullColumnOfTypeWithDefault(
      String fullTable, String column, String enumType, String defaultValue) throws Exception {
    var parts = splitSchemaTable(fullTable);
    try (var conn = openConnection("njall");
        var stmt = conn.prepareStatement(COLUMN_DETAILS_SQL)) {
      stmt.setString(1, parts.getFirst());
      stmt.setString(2, parts.get(1));
      stmt.setString(3, column);
      try (var rs = stmt.executeQuery()) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("is_nullable")).isEqualTo("NO");
        assertThat(rs.getString("udt_schema")).isEqualTo("njall_users");
        assertThat(rs.getString("udt_name")).isEqualTo(enumType);
        assertThat(rs.getString("column_default")).startsWith("'" + defaultValue + "'");
      }
    }
  }

  @Then(
      "row level security isolates rows in {string}, {string}, and {string} by active tenant"
          + " setting")
  public void rowLevelSecurityIsolatesRowsByActiveTenantSetting(
      String table1, String table2, String table3) throws Exception {
    assertThat(List.of(table1, table2, table3))
        .containsExactlyInAnyOrder(
            "njall_users.systems", "njall_users.campaigns", "njall_users.games");
    var tenantOne = newTenantFixture();
    var tenantTwo = newTenantFixture();
    try (var adminConn = openConnection("njall");
        var usersConn = openConnection("njall_users")) {
      usersConn.setAutoCommit(false);
      try {
        insertTenantFixture(
            adminConn, tenantOne, "RLS System One", "RLS Campaign One", "RLS Game 1");
        insertTenantFixture(
            adminConn, tenantTwo, "RLS System Two", "RLS Campaign Two", "RLS Game 2");
        assertUnsetTenantVisibility(usersConn, tenantOne, tenantTwo);
        assertTenantVisibility(usersConn, tenantOne, tenantTwo);
        assertTenantVisibility(usersConn, tenantTwo, tenantOne);
      } finally {
        cleanupTenants(adminConn, tenantOne.tenantId(), tenantTwo.tenantId());
      }
    }
  }

  @Then(
      "unique index {string} enforces tenant-wide case-insensitive uniqueness on system names"
          + " within a tenant")
  public void uniqueIndexEnforcesCaseInsensitiveSystemNameUniqueness(String indexName)
      throws Exception {
    try (var conn = openConnection("njall")) {
      assertThat(indexExists(conn, "njall_users", "systems", indexName)).isTrue();
      conn.setAutoCommit(false);
      try {
        var tenantId = UUID.randomUUID();
        var actorId = UUID.randomUUID();
        var firstSystemId = UUID.randomUUID();
        var secondSystemId = UUID.randomUUID();
        insertStudioAndActor(conn, tenantId, actorId, "Unique Index Studio");
        insertEntity(conn, firstSystemId, tenantId, "System", actorId);
        insertSystemRow(conn, tenantId, firstSystemId, "Call of Cthulhu");
        insertEntity(conn, secondSystemId, tenantId, "System", actorId);
        assertThatThrownBy(() -> insertSystemRow(conn, tenantId, secondSystemId, "call of cthulhu"))
            .isInstanceOf(SQLException.class)
            .satisfies(ex -> assertSqlFailure((SQLException) ex, "23505", indexName));
      } finally {
        conn.rollback();
      }
    }
  }

  @Then("composite foreign key {string} rejects cross-tenant system references on {string}")
  public void compositeForeignKeyRejectsCrossTenantSystemReferences(
      String constraintName, String fullTable) throws Exception {
    var parts = splitSchemaTable(fullTable);
    try (var conn = openConnection("njall")) {
      assertThat(constraintExists(conn, parts.getFirst(), parts.get(1), constraintName, "f"))
          .isTrue();
      conn.setAutoCommit(false);
      try {
        var tenantOne = UUID.randomUUID();
        var actorOne = UUID.randomUUID();
        var gameOne = UUID.randomUUID();
        var tenantTwo = UUID.randomUUID();
        var actorTwo = UUID.randomUUID();
        var systemTwo = UUID.randomUUID();
        insertStudioAndActor(conn, tenantOne, actorOne, "Cross Tenant Game Studio");
        insertEntity(conn, gameOne, tenantOne, "Game", actorOne);
        insertEventRow(conn, tenantOne, gameOne, "Cross Tenant Game", "GAME");
        insertStudioAndActor(conn, tenantTwo, actorTwo, "Cross Tenant System Studio");
        insertEntity(conn, systemTwo, tenantTwo, "System", actorTwo);
        insertSystemRow(conn, tenantTwo, systemTwo, "Tenant Two System");
        assertThatThrownBy(
                () -> insertGameRow(conn, tenantOne, gameOne, systemTwo, Optional.empty()))
            .isInstanceOf(SQLException.class)
            .satisfies(ex -> assertSqlFailure((SQLException) ex, "23503", constraintName));
      } finally {
        conn.rollback();
      }
    }
  }

  private static TenantFixture newTenantFixture() {
    return new TenantFixture(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID());
  }

  private static List<String> splitSchemaTable(String fullTable) {
    return Splitter.on('.').splitToList(fullTable);
  }

  private static Connection openConnection(String role) throws SQLException {
    return DriverManager.getConnection(
        DatabaseMigrationSteps.getJdbcUrl(), role, DatabaseMigrationSteps.getPasswordFor(role));
  }

  private static void insertTenantFixture(
      Connection conn,
      TenantFixture fixture,
      String systemName,
      String campaignTitle,
      String gameTitle)
      throws SQLException {
    insertStudioAndActor(conn, fixture.tenantId(), fixture.actorId(), "RLS Studio " + systemName);
    insertEntity(conn, fixture.systemId(), fixture.tenantId(), "System", fixture.actorId());
    insertSystemRow(conn, fixture.tenantId(), fixture.systemId(), systemName);
    insertEntity(conn, fixture.campaignId(), fixture.tenantId(), "Campaign", fixture.actorId());
    insertCampaignRow(
        conn, fixture.tenantId(), fixture.campaignId(), fixture.systemId(), campaignTitle);
    insertEntity(conn, fixture.gameId(), fixture.tenantId(), "Game", fixture.actorId());
    insertEventRow(conn, fixture.tenantId(), fixture.gameId(), gameTitle, "GAME");
    insertGameRow(
        conn,
        fixture.tenantId(),
        fixture.gameId(),
        fixture.systemId(),
        Optional.of(fixture.campaignId()));
  }

  private static void assertTenantVisibility(
      Connection usersConn, TenantFixture active, TenantFixture other) throws SQLException {
    try {
      setActiveTenant(usersConn, active.tenantId());
      assertThat(querySystemIds(usersConn))
          .contains(active.systemId())
          .doesNotContain(other.systemId());
      assertThat(queryCampaignIds(usersConn))
          .contains(active.campaignId())
          .doesNotContain(other.campaignId());
      assertThat(queryGameIds(usersConn)).contains(active.gameId()).doesNotContain(other.gameId());
    } finally {
      usersConn.rollback();
    }
  }

  private static void assertUnsetTenantVisibility(
      Connection usersConn, TenantFixture tenantOne, TenantFixture tenantTwo) throws SQLException {
    try {
      assertThat(querySystemIds(usersConn))
          .doesNotContain(tenantOne.systemId(), tenantTwo.systemId());
      assertThat(queryCampaignIds(usersConn))
          .doesNotContain(tenantOne.campaignId(), tenantTwo.campaignId());
      assertThat(queryGameIds(usersConn)).doesNotContain(tenantOne.gameId(), tenantTwo.gameId());
    } finally {
      usersConn.rollback();
    }
  }

  private static void setActiveTenant(Connection conn, UUID tenantId) throws SQLException {
    try (var stmt = conn.prepareStatement(SET_TENANT_SQL)) {
      stmt.setString(1, tenantId.toString());
      try (var rs = stmt.executeQuery()) {
        assertThat(rs.next()).isTrue();
      }
    }
  }

  private static ImmutableSet<UUID> querySystemIds(Connection conn) throws SQLException {
    return queryIdSet(conn, SELECT_SYSTEM_IDS_SQL);
  }

  private static ImmutableSet<UUID> queryCampaignIds(Connection conn) throws SQLException {
    return queryIdSet(conn, SELECT_CAMPAIGN_IDS_SQL);
  }

  private static ImmutableSet<UUID> queryGameIds(Connection conn) throws SQLException {
    return queryIdSet(conn, SELECT_GAME_IDS_SQL);
  }

  private static ImmutableSet<UUID> queryIdSet(Connection conn, String sql) throws SQLException {
    var ids = ImmutableSet.<UUID>builder();
    try (var stmt = conn.prepareStatement(sql);
        var rs = stmt.executeQuery()) {
      while (rs.next()) {
        ids.add(rs.getObject("id", UUID.class));
      }
    }
    return ids.build();
  }

  private static void cleanupTenants(Connection conn, UUID tenantOne, UUID tenantTwo)
      throws SQLException {
    deleteTenantRows(conn, tenantOne);
    deleteTenantRows(conn, tenantTwo);
  }

  private static void deleteTenantRows(Connection conn, UUID tenantId) throws SQLException {
    executeDelete(conn, DELETE_GAMES_SQL, tenantId);
    executeDelete(conn, DELETE_EVENTS_SQL, tenantId);
    executeDelete(conn, DELETE_CAMPAIGNS_SQL, tenantId);
    executeDelete(conn, DELETE_SYSTEMS_SQL, tenantId);
    executeDelete(conn, DELETE_ENTITIES_SQL, tenantId);
    executeDelete(conn, DELETE_STUDIO_SQL, tenantId);
  }

  private static void executeDelete(Connection conn, String sql, UUID id) throws SQLException {
    try (var stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);
      stmt.executeUpdate();
    }
  }

  private static void insertStudioAndActor(
      Connection conn, UUID tenantId, UUID actorId, String studioName) throws SQLException {
    try (var stmt = conn.prepareStatement(INSERT_STUDIO_SQL)) {
      stmt.setObject(1, tenantId);
      stmt.setString(2, studioName);
      stmt.executeUpdate();
    }
    insertEntity(conn, actorId, tenantId, "Actor", actorId);
  }

  private static void insertEntity(
      Connection conn, UUID id, UUID tenantId, String entityType, UUID generatorId)
      throws SQLException {
    try (var stmt = conn.prepareStatement(INSERT_ENTITY_SQL)) {
      stmt.setObject(1, id);
      stmt.setObject(2, tenantId);
      stmt.setString(3, entityType);
      stmt.setObject(4, generatorId);
      stmt.executeUpdate();
    }
  }

  private static void insertSystemRow(Connection conn, UUID tenantId, UUID systemId, String name)
      throws SQLException {
    try (var stmt = conn.prepareStatement(INSERT_SYSTEM_SQL)) {
      stmt.setObject(1, tenantId);
      stmt.setObject(2, systemId);
      stmt.setString(3, name);
      stmt.executeUpdate();
    }
  }

  private static void insertCampaignRow(
      Connection conn, UUID tenantId, UUID campaignId, UUID systemId, String title)
      throws SQLException {
    try (var stmt = conn.prepareStatement(INSERT_CAMPAIGN_SQL)) {
      stmt.setObject(1, tenantId);
      stmt.setObject(2, campaignId);
      stmt.setObject(3, systemId);
      stmt.setString(4, title);
      stmt.executeUpdate();
    }
  }

  private static void insertEventRow(
      Connection conn, UUID tenantId, UUID eventId, String title, String eventType)
      throws SQLException {
    try (var stmt = conn.prepareStatement(INSERT_EVENT_SQL)) {
      stmt.setObject(1, tenantId);
      stmt.setObject(2, eventId);
      stmt.setString(3, title);
      stmt.setString(4, eventType);
      stmt.executeUpdate();
    }
  }

  private static void insertGameRow(
      Connection conn, UUID tenantId, UUID gameId, UUID systemId, Optional<UUID> campaignId)
      throws SQLException {
    try (var stmt = conn.prepareStatement(INSERT_GAME_SQL)) {
      stmt.setObject(1, tenantId);
      stmt.setObject(2, gameId);
      stmt.setObject(3, systemId);
      if (campaignId.isPresent()) {
        stmt.setObject(4, campaignId.orElseThrow());
      } else {
        stmt.setNull(4, Types.OTHER);
      }
      stmt.executeUpdate();
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

  private static void assertSqlFailure(
      SQLException exception, String expectedSqlState, String expectedConstraint) {
    assertThat(exception.getSQLState()).isEqualTo(expectedSqlState);
    assertThat(exception.getMessage()).contains(expectedConstraint);
  }
}
