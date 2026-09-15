package com.larpconnect.njall.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import com.larpconnect.njall.data.config.MigrationConfig;
import com.larpconnect.njall.data.migration.DatabaseMigrator;
import com.larpconnect.njall.server.ServerApp;
import com.larpconnect.njall.server.ServerModule;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
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

  private MigrationConfig migrationConfig;
  private int lastMigrationsCount;
  private Integer lastServerExitCode;

  @BeforeAll
  public static void setUpContainer() throws Exception {
    POSTGRES.start();
    provisionRoles();
  }

  @AfterAll
  public static void tearDownContainer() {
    POSTGRES.stop();
  }

  private static void provisionRoles() throws Exception {
    try (var conn = openConnection("postgres", "postgres");
        var stmt = conn.createStatement()) {
      stmt.execute("CREATE ROLE njall WITH LOGIN SUPERUSER PASSWORD 'njall';");
      stmt.execute("CREATE ROLE njall_admin WITH LOGIN PASSWORD 'njall_admin';");
      stmt.execute("CREATE ROLE njall_users WITH LOGIN PASSWORD 'njall_users';");
      stmt.execute("CREATE ROLE njall_system WITH LOGIN PASSWORD 'njall_system';");
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
    assertThat(arg).isEqualTo("--migrate");
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
    var injector = createInjector(createDefaultConfig());
    var app = new ServerApp(() -> injector, inj -> {});
    lastServerExitCode = app.runWithArgs(new String[] {"--migrate"});
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
    try (var conn = openConnection("njall", "njall");
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
    assertCanSelectFromServers(role, role);
  }

  @Then("role {string} has USAGE on {string}, {string} and SELECT on all tables in {string}")
  public void roleHasUsageAndSelectOnNjallShort(
      String role, String s1, String s2, String tableSchema) throws Exception {
    assertCanSelectFromServers(role, role);
    assertCannotInsertIntoServers(role, role);
  }

  @Then("table {string} contains a server with name {string} and primary domain {string}")
  public void tableContainsAServerWithNameAndPrimaryDomain(
      String table, String expectedName, String expectedDomain) throws Exception {
    var query = "SELECT id, name, primary_domain FROM njall.servers WHERE name = ?";
    try (var conn = openConnection("njall", "njall");
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
    var query =
        "SELECT id, role_type, contact_type, contact FROM njall.server_contacts WHERE contact = ?";
    try (var conn = openConnection("njall", "njall");
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
    try (var conn = openConnection("njall", "njall");
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
        "njall",
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
                Map.of(
                    "larpconnect.data.database.migration.jdbc-url",
                    migrationConfig.jdbcUrl(),
                    "larpconnect.data.database.migration.username",
                    migrationConfig.username(),
                    "larpconnect.data.database.migration.password",
                    migrationConfig.password(),
                    "larpconnect.server.name",
                    migrationConfig.placeholders().get("server_name"),
                    "larpconnect.server.primary-domain",
                    migrationConfig.placeholders().get("primary_domain"),
                    "larpconnect.server.admin-contact",
                    migrationConfig.placeholders().get("admin_contact")))
            .withFallback(ConfigFactory.load());

    return Guice.createInjector(
        Modules.override(new ServerModule())
            .with(
                new AbstractModule() {
                  @Override
                  protected void configure() {
                    bind(Config.class).toInstance(customConfig);
                  }
                }));
  }
}
