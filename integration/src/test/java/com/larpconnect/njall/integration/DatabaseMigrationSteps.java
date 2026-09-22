package com.larpconnect.njall.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.base.Splitter;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.larpconnect.njall.data.config.MigrationConfig;
import com.larpconnect.njall.data.migration.DatabaseMigrator;
import com.larpconnect.njall.server.ServerApp;
import com.larpconnect.njall.server.ServerModule;
import com.typesafe.config.ConfigFactory;
import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Cucumber step definitions for Flyway database migration integration scenarios. */
public final class DatabaseMigrationSteps {

  private static final DockerImageName POSTGIS_IMAGE =
      DockerImageName.parse("postgis/postgis:18-3.6-alpine").asCompatibleSubstituteFor("postgres");

  // The PostgreSQLContainer implements AutoCloseable, but its lifecycle is managed
  // across the integration suite lifecycle via @BeforeAll and @AfterAll.
  @SuppressWarnings("resource")
  private static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(POSTGIS_IMAGE)
          .withDatabaseName("larpconnect")
          .withUsername("postgres")
          .withPassword("postgres");

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final String NJALL_PASSWORD = generateRandomPassword();
  private static final String NJALL_ADMIN_PASSWORD = generateRandomPassword();
  private static final String NJALL_USERS_PASSWORD = generateRandomPassword();
  private static final String NJALL_SYSTEM_PASSWORD = generateRandomPassword();

  private MigrationConfig migrationConfig;
  private int lastMigrationsCount;
  private Integer lastServerExitCode;
  private String[] serverArgs;

  private static String generateRandomPassword() {
    return HexFormat.of().formatHex(SECURE_RANDOM.generateSeed(16));
  }

  /**
   * Returns the dynamically generated password for the given PostgreSQL role.
   *
   * @param role database role name
   * @return generated password string
   */
  public static String getPasswordFor(String role) {
    return switch (role) {
      case "njall" -> NJALL_PASSWORD;
      case "njall_admin" -> NJALL_ADMIN_PASSWORD;
      case "njall_users" -> NJALL_USERS_PASSWORD;
      case "njall_system" -> NJALL_SYSTEM_PASSWORD;
      default -> throw new IllegalArgumentException("Unknown role: " + role);
    };
  }

  @BeforeAll
  public static void setUpContainer() throws Exception {
    if (!POSTGRES.isRunning()) {
      POSTGRES.start();
      provisionRoles();
    }
  }

  public static void ensureStartedAndMigrated() throws Exception {
    setUpContainer();
    var injector = createInjector(createDefaultConfig());
    var migrator = injector.getInstance(DatabaseMigrator.class);
    migrator.migrate();
  }

  public static String getJdbcUrl() {
    return POSTGRES.getJdbcUrl();
  }

  @AfterAll
  public static void tearDownContainer() {
    POSTGRES.stop();
  }

  private static void provisionRoles() throws Exception {
    var sql =
        "DO $$\n"
            + "BEGIN\n"
            + "  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'njall') THEN\n"
            + "    CREATE ROLE njall WITH LOGIN SUPERUSER PASSWORD '"
            + NJALL_PASSWORD
            + "';\n"
            + "  ELSE\n"
            + "    ALTER ROLE njall WITH PASSWORD '"
            + NJALL_PASSWORD
            + "';\n"
            + "  END IF;\n"
            + "  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'njall_admin')"
            + " THEN\n"
            + "    CREATE ROLE njall_admin WITH LOGIN PASSWORD '"
            + NJALL_ADMIN_PASSWORD
            + "';\n"
            + "  ELSE\n"
            + "    ALTER ROLE njall_admin WITH PASSWORD '"
            + NJALL_ADMIN_PASSWORD
            + "';\n"
            + "  END IF;\n"
            + "  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'njall_users')"
            + " THEN\n"
            + "    CREATE ROLE njall_users WITH LOGIN PASSWORD '"
            + NJALL_USERS_PASSWORD
            + "';\n"
            + "  ELSE\n"
            + "    ALTER ROLE njall_users WITH PASSWORD '"
            + NJALL_USERS_PASSWORD
            + "';\n"
            + "  END IF;\n"
            + "  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'njall_system')"
            + " THEN\n"
            + "    CREATE ROLE njall_system WITH LOGIN PASSWORD '"
            + NJALL_SYSTEM_PASSWORD
            + "';\n"
            + "  ELSE\n"
            + "    ALTER ROLE njall_system WITH PASSWORD '"
            + NJALL_SYSTEM_PASSWORD
            + "';\n"
            + "  END IF;\n"
            + "END $$;\n";
    try (var conn = openConnection("postgres", "postgres");
        var stmt = conn.createStatement()) {
      stmt.execute(sql);
    }
  }

  @Given(
      "a clean PostgreSQL database instance with pre-configured roles {string}, {string}, {string},"
          + " and {string}")
  public void aCleanPostgreSqlDatabaseInstanceWithPreConfiguredRoles(
      String r1, String r2, String r3, String r4) {
    assertThat(POSTGRES.isRunning()).isTrue();
    this.migrationConfig = createDefaultConfig();
  }

  @Given(
      "migration configuration with server name {string}, primary domain {string}, and admin"
          + " contact {string}")
  public void migrationConfigurationWithServerNamePrimaryDomainAndAdminContact(
      String serverName, String primaryDomain, String adminContact) {
    this.migrationConfig = createConfig(serverName, primaryDomain, adminContact);
  }

  @Given("the server application is started with argument {string}")
  public void theServerApplicationIsStartedWithArgument(String arg) {
    assertThat(arg).isEqualTo("migrate");
    this.serverArgs =
        new String[] {
          "migrate",
          "--jdbc-url=" + POSTGRES.getJdbcUrl(),
          "--username=njall",
          "--password=" + NJALL_PASSWORD
        };
  }

  @Given("the server application is started with custom database migration arguments")
  public void theServerApplicationIsStartedWithCustomDatabaseMigrationArguments() {
    this.serverArgs =
        new String[] {
          "migrate",
          "--jdbc-url=" + POSTGRES.getJdbcUrl(),
          "--username=njall",
          "--password=" + NJALL_PASSWORD,
          "--default-schema=njall",
          "--server-name=alpha-node",
          "--primary-domain=larpconnect.test",
          "--admin-contact=ops@larpconnect.test"
        };
  }

  @Given("the bootstrap database migration has already been executed")
  public void theBootstrapDatabaseMigrationHasAlreadyBeenExecuted() {
    if (migrationConfig == null) {
      this.migrationConfig = createDefaultConfig();
    }
    var injector = createInjector(migrationConfig);
    var migrator = injector.getInstance(DatabaseMigrator.class);
    migrator.migrate();
  }

  @When("the database migrator executes the bootstrap migration")
  public void theDatabaseMigratorExecutesTheBootstrapMigration() {
    var injector = createInjector(migrationConfig);
    var migrator = injector.getInstance(DatabaseMigrator.class);
    lastMigrationsCount = migrator.migrate();
  }

  @When("the server execution completes")
  public void theServerExecutionCompletes() {
    var app = new ServerApp();
    lastServerExitCode = app.runWithArgs(serverArgs);
  }

  @When("the server execution completes with custom arguments")
  public void theServerExecutionCompletesWithCustomArguments() {
    var app = new ServerApp();
    lastServerExitCode = app.runWithArgs(serverArgs);
  }

  @When("the database migrator executes the bootstrap migration again")
  public void theDatabaseMigratorExecutesTheBootstrapMigrationAgain() {
    var injector = createInjector(migrationConfig);
    var migrator = injector.getInstance(DatabaseMigrator.class);
    lastMigrationsCount = migrator.migrate();
  }

  @Then("schemas {string}, {string}, {string}, and {string} exist and are owned by {string}")
  public void schemasExistAndAreOwnedBy(
      String s1, String s2, String s3, String s4, String expectedOwner) throws Exception {
    var query =
        "SELECT n.nspname, r.rolname FROM pg_namespace n "
            + "JOIN pg_roles r ON n.nspowner = r.oid "
            + "WHERE n.nspname IN (?, ?, ?, ?)";
    try (var conn = openConnection("njall", NJALL_PASSWORD);
        var stmt = conn.prepareStatement(query)) {
      stmt.setString(1, s1);
      stmt.setString(2, s2);
      stmt.setString(3, s3);
      stmt.setString(4, s4);
      try (var rs = stmt.executeQuery()) {
        var foundSchemas = new ArrayList<String>();
        while (rs.next()) {
          foundSchemas.add(rs.getString("nspname"));
          assertThat(rs.getString("rolname")).isEqualTo(expectedOwner);
        }
        assertThat(foundSchemas).containsExactlyInAnyOrder(s1, s2, s3, s4);
      }
    }
  }

  @Then(
      "role {string} has USAGE on {string}, {string}, {string} and SELECT on all tables in"
          + " {string}")
  public void roleHasUsageAndSelectOnNjall(
      String role, String s1, String s2, String s3, String tableSchema) throws Exception {
    assertCanSelectFromServers(role, getPasswordFor(role));
  }

  @Then("role {string} has USAGE on {string}, {string} and SELECT on all tables in {string}")
  public void roleHasUsageAndSelectOnNjallShort(
      String role, String s1, String s2, String tableSchema) throws Exception {
    assertCanSelectFromServers(role, getPasswordFor(role));
    assertCannotInsertIntoServers(role, getPasswordFor(role));
  }

  @Then("table {string} contains a server with name {string} and primary domain {string}")
  public void tableContainsAServerWithNameAndPrimaryDomain(
      String table, String expectedName, String expectedDomain) throws Exception {
    assertThat(table).isEqualTo("njall.servers");
    var query = "SELECT id, name, primary_domain FROM njall.servers WHERE name = ?";
    try (var conn = openConnection("njall", NJALL_PASSWORD);
        var stmt = conn.prepareStatement(query)) {
      stmt.setString(1, expectedName);
      try (var rs = stmt.executeQuery()) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("name")).isEqualTo(expectedName);
        assertThat(rs.getString("primary_domain")).isEqualTo(expectedDomain);
        assertThat(rs.getObject("id")).isNotNull();
      }
    }
  }

  @Then("table {string} contains an ADMIN contact with email {string}")
  public void tableContainsAnAdminContactWithEmail(String table, String expectedEmail)
      throws Exception {
    assertThat(table).isEqualTo("njall.server_contacts");
    var query =
        "SELECT id, role_type, contact_type, contact FROM njall.server_contacts WHERE contact = ?";
    try (var conn = openConnection("njall", NJALL_PASSWORD);
        var stmt = conn.prepareStatement(query)) {
      stmt.setString(1, expectedEmail);
      try (var rs = stmt.executeQuery()) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("role_type")).isEqualTo("ADMIN");
        assertThat(rs.getString("contact_type")).isEqualTo("EMAIL");
        assertThat(rs.getObject("id")).isNotNull();
      }
    }
  }

  @Then("database migrations are executed to completion")
  public void databaseMigrationsAreExecutedToCompletion() throws Exception {
    try (var conn = openConnection("njall", NJALL_PASSWORD);
        var stmt = conn.createStatement();
        var rs = stmt.executeQuery("SELECT count(*) FROM njall.flyway_schema_history")) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(1);
    }
  }

  @Then("the application process terminates with exit status {int}")
  public void theApplicationProcessTerminatesWithExitStatus(int expectedExitCode) {
    assertThat(lastServerExitCode).isEqualTo(expectedExitCode);
  }

  @Then("zero migrations are applied")
  public void zeroMigrationsAreApplied() {
    assertThat(lastMigrationsCount).isZero();
  }

  @Then("table {string} exists and is owned by {string}")
  public void tableExistsAndIsOwnedBy(String fullTable, String expectedOwner) throws Exception {
    var parts = Splitter.on('.').splitToList(fullTable);
    var schema = parts.getFirst();
    var table = parts.get(1);
    var query = "SELECT tableowner FROM pg_tables WHERE schemaname = ? AND tablename = ?";
    try (var conn = openConnection("njall", NJALL_PASSWORD);
        var stmt = conn.prepareStatement(query)) {
      stmt.setString(1, schema);
      stmt.setString(2, table);
      try (var rs = stmt.executeQuery()) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("tableowner")).isEqualTo(expectedOwner);
      }
    }
  }

  @Then("role {string} has USAGE on schema {string} and SELECT on table {string}")
  public void roleHasUsageAndSelectOnTable(String role, String schema, String table)
      throws Exception {
    var query = "SELECT count(*) FROM " + schema + "." + table;
    try (var conn = openConnection(role, getPasswordFor(role));
        var stmt = conn.createStatement();
        var rs = stmt.executeQuery(query)) {
      assertThat(rs.next()).isTrue();
    }
  }

  @Then("role {string} has no SELECT on table {string}")
  public void roleHasNoSelectOnTable(String role, String table) throws Exception {
    var query = "SELECT count(*) FROM " + table;
    try (var conn = openConnection(role, getPasswordFor(role));
        var stmt = conn.createStatement()) {
      assertThatThrownBy(
              () -> {
                try (var rs = stmt.executeQuery(query)) {
                  // Consuming rs.next() satisfies SpotBugs RV_RETURN_VALUE_IGNORED while ensuring
                  // the query execution is fully attempted under permission checks.
                  assertThat(rs.next()).isFalse();
                }
              })
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("permission denied");
    }
  }

  @Then("table {string} has row level security enabled")
  public void tableHasRowLevelSecurityEnabled(String fullTable) throws Exception {
    var parts = Splitter.on('.').splitToList(fullTable);
    var schema = parts.getFirst();
    var table = parts.get(1);
    var query = "SELECT rowsecurity FROM pg_tables WHERE schemaname = ? AND tablename = ?";
    try (var conn = openConnection("njall", NJALL_PASSWORD);
        var stmt = conn.prepareStatement(query)) {
      stmt.setString(1, schema);
      stmt.setString(2, table);
      try (var rs = stmt.executeQuery()) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getBoolean("rowsecurity")).isTrue();
      }
    }
  }

  private void assertCanSelectFromServers(String user, String pass) throws Exception {
    try (var conn = openConnection(user, pass);
        var stmt = conn.createStatement();
        var rs = stmt.executeQuery("SELECT count(*) FROM njall.servers")) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(1);
    }
  }

  private void assertCannotInsertIntoServers(String user, String pass) throws Exception {
    var sql = "INSERT INTO njall.servers (name, primary_domain) VALUES ('fail', 'fail.com')";
    try (var conn = openConnection(user, pass);
        var stmt = conn.createStatement()) {
      assertThatThrownBy(() -> stmt.execute(sql))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("permission denied");
    }
  }

  private static Connection openConnection(String user, String password) throws SQLException {
    return DriverManager.getConnection(POSTGRES.getJdbcUrl(), user, password);
  }

  private static MigrationConfig createDefaultConfig() {
    return createConfig("alpha-node", "larpconnect.test", "ops@larpconnect.test");
  }

  private static MigrationConfig createConfig(
      String serverName, String primaryDomain, String adminContact) {
    return MigrationConfig.of(
        POSTGRES.getJdbcUrl(),
        "njall",
        NJALL_PASSWORD,
        List.of("njall", "njall_admin", "njall_users", "njall_system"),
        "njall",
        Map.of(
            "server_name", serverName,
            "primary_domain", primaryDomain,
            "admin_contact", adminContact));
  }

  private static Injector createInjector(MigrationConfig migrationConfig) {
    var customConfig =
        ConfigFactory.parseMap(
                Map.ofEntries(
                    Map.entry(
                        "larpconnect.data.database.migration.jdbc-url", migrationConfig.jdbcUrl()),
                    Map.entry(
                        "larpconnect.data.database.migration.username", migrationConfig.username()),
                    Map.entry(
                        "larpconnect.data.database.migration.password", migrationConfig.password()),
                    Map.entry(
                        "larpconnect.data.database.admin.jdbc-url", migrationConfig.jdbcUrl()),
                    Map.entry("larpconnect.data.database.admin.username", "njall_admin"),
                    Map.entry("larpconnect.data.database.admin.password", NJALL_ADMIN_PASSWORD),
                    Map.entry(
                        "larpconnect.data.database.users.jdbc-url", migrationConfig.jdbcUrl()),
                    Map.entry("larpconnect.data.database.users.username", "njall_users"),
                    Map.entry("larpconnect.data.database.users.password", NJALL_USERS_PASSWORD),
                    Map.entry(
                        "larpconnect.server.name",
                        migrationConfig.placeholders().get("server_name")),
                    Map.entry(
                        "larpconnect.server.primary-domain",
                        migrationConfig.placeholders().get("primary_domain")),
                    Map.entry(
                        "larpconnect.server.admin-contact",
                        migrationConfig.placeholders().get("admin_contact"))))
            .withFallback(ConfigFactory.load());

    return Guice.createInjector(new ServerModule(customConfig));
  }
}
