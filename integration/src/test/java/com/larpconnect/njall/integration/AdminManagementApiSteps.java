package com.larpconnect.njall.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.larpconnect.njall.server.ServerModule;
import com.larpconnect.njall.server.http.HttpServerService;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.typed.ActorSystem;

/** Cucumber step definitions for administrative management REST API integration flows. */
public final class AdminManagementApiSteps {

  private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

  private final Map<String, String> rememberedIds = new HashMap<>();
  private HttpServerService serverService;
  private ActorSystem<Void> system;
  private int boundPort;
  private HttpResponse<String> response;
  private JsonNode rootJson;

  @Before("@AdminApi")
  public void setUp() throws Exception {
    DatabaseMigrationSteps.ensureStartedAndMigrated();
    truncateAdminTables();
  }

  @After("@AdminApi")
  public void tearDown() throws Exception {
    if (serverService != null) {
      serverService.stop().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }
    if (system != null) {
      system.terminate();
      system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }
  }

  @Given("the admin HTTP server is running with database migrations applied")
  public void theAdminHttpServerIsRunningWithDatabaseMigrationsApplied() throws Exception {
    var config = buildIntegrationConfig(DatabaseMigrationSteps.getJdbcUrl());
    var injector = Guice.createInjector(new ServerModule(config));

    serverService = injector.getInstance(HttpServerService.class);
    system = injector.getInstance(Key.get(new TypeLiteral<ActorSystem<Void>>() {}));

    serverService.start().toCompletableFuture().get(10, TimeUnit.SECONDS);
    boundPort = serverService.getBoundPort();
    assertThat(boundPort).isPositive();
  }

  @When("an API admin sends a POST request to {string} with body:")
  public void anApiAdminSendsAPostRequestToWithBody(String path, String docString)
      throws Exception {
    var resolvedPath = resolveVariables(path);
    var resolvedBody = resolveVariables(docString);
    var client = HttpClient.newHttpClient();
    var request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + boundPort + resolvedPath))
            .header("Content-Type", "application/json")
            .timeout(REQUEST_TIMEOUT)
            .POST(HttpRequest.BodyPublishers.ofString(resolvedBody))
            .build();

    response = client.send(request, HttpResponse.BodyHandlers.ofString());
    parseResponseBody();
  }

  @When("an API admin sends a GET request to {string}")
  public void anApiAdminSendsAGetRequestTo(String path) throws Exception {
    var resolvedPath = resolveVariables(path);
    var client = HttpClient.newHttpClient();
    var request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + boundPort + resolvedPath))
            .timeout(REQUEST_TIMEOUT)
            .GET()
            .build();

    response = client.send(request, HttpResponse.BodyHandlers.ofString());
    parseResponseBody();
  }

  @Then("the HTTP response status code is {int}")
  public void theHttpResponseStatusCodeIs(int expectedStatus) {
    assertThat(response.statusCode())
        .withFailMessage(
            "Expected status %d but was %d with body: %s",
            expectedStatus, response.statusCode(), response.body())
        .isEqualTo(expectedStatus);
  }

  @Then("the HTTP response content-type contains {string}")
  public void theHttpResponseContentTypeContains(String expectedType) {
    var contentType = response.headers().firstValue("Content-Type").orElse("");
    assertThat(contentType).contains(expectedType);
  }

  @Then("the JSON response contains field {string} with value {string}")
  public void theJsonResponseContainsFieldWithValue(String field, String expectedValue) {
    assertJsonBodyPresent();
    assertThat(rootJson.path(field).asText()).isEqualTo(expectedValue);
  }

  @Then("the response field {string} is remembered as {string}")
  public void theResponseFieldIsRememberedAs(String field, String key) {
    assertJsonBodyPresent();
    var node = rootJson.path(field);
    var value = node.isMissingNode() ? rootJson.path("id").asText() : node.asText();
    assertThat(value).isNotBlank();
    rememberedIds.put(key, value);
  }

  @Then("the JSON response array contains an item with {string} equal to {string}")
  public void theJsonResponseArrayContainsAnItemWithEqualTo(String field, String expectedValue) {
    assertJsonBodyPresent();
    assertThat(rootJson.isArray()).isTrue();
    var found = false;
    for (var node : rootJson) {
      if (expectedValue.equals(node.path(field).asText())) {
        found = true;
        break;
      }
    }
    assertThat(found)
        .withFailMessage("Array does not contain item with %s=%s", field, expectedValue)
        .isTrue();
  }

  @Then("the JSON response array {string} contains {string}")
  public void theJsonResponseArrayContains(String arrayField, String expectedValue) {
    assertJsonBodyPresent();
    var arrayNode = rootJson.path(arrayField);
    assertThat(arrayNode.isArray()).isTrue();
    var found = false;
    for (var node : arrayNode) {
      if (nodeMatchesValue(node, expectedValue)) {
        found = true;
        break;
      }
    }
    assertThat(found)
        .withFailMessage(
            "Array %s does not contain %s. Elements: %s", arrayField, expectedValue, arrayNode)
        .isTrue();
  }

  @Then("the JSON response array {string} does not contain {string}")
  public void theJsonResponseArrayDoesNotContain(String arrayField, String expectedValue) {
    assertJsonBodyPresent();
    var arrayNode = rootJson.path(arrayField);
    assertThat(arrayNode.isArray()).isTrue();
    var found = false;
    for (var node : arrayNode) {
      if (nodeMatchesValue(node, expectedValue)) {
        found = true;
        break;
      }
    }
    assertThat(found)
        .withFailMessage(
            "Array %s unexpectedly contains %s. Elements: %s", arrayField, expectedValue, arrayNode)
        .isFalse();
  }

  @Then("the JSON response error has code {int} and non-empty message")
  public void theJsonResponseErrorHasCodeAndNonEmptyMessage(int expectedCode) {
    assertJsonBodyPresent();
    assertThat(rootJson.path("code").asInt()).isEqualTo(expectedCode);
    assertThat(rootJson.path("message").asText()).isNotBlank();
  }

  private boolean nodeMatchesValue(JsonNode node, String expectedValue) {
    if (node.isTextual() && expectedValue.equals(node.asText())) {
      return true;
    }
    if (node.isObject()) {
      for (var field : node) {
        if (expectedValue.equals(field.asText())) {
          return true;
        }
      }
    }
    return false;
  }

  private void assertJsonBodyPresent() {
    assertThat(rootJson)
        .withFailMessage("Expected valid JSON response body but was: %s", response.body())
        .isNotNull();
  }

  private void parseResponseBody() throws Exception {
    if (response.body() != null && !response.body().isBlank()) {
      try {
        rootJson = MAPPER.readTree(response.body());
      } catch (Exception e) {
        rootJson = null;
      }
    } else {
      rootJson = null;
    }
  }

  private String resolveVariables(String input) {
    var result = input;
    for (var entry : rememberedIds.entrySet()) {
      result = result.replace("{" + entry.getKey() + "}", entry.getValue());
    }
    return result;
  }

  private static void truncateAdminTables() throws Exception {
    var sql =
        "TRUNCATE TABLE njall_admin.admin_role_assignments, "
            + "njall_admin.admin_users, "
            + "njall_admin.admin_roles, "
            + "njall_admin.studios_lookup CASCADE";
    try (var conn =
            DriverManager.getConnection(DatabaseMigrationSteps.getJdbcUrl(), "njall", "njall");
        var stmt = conn.createStatement()) {
      stmt.execute(sql);
    }
  }

  private static Config buildIntegrationConfig(String jdbcUrl) {
    var configMap =
        Map.of(
            "larpconnect.server.host",
            "127.0.0.1",
            "larpconnect.server.port",
            0,
            "larpconnect.data.database.admin.jdbc-url",
            jdbcUrl,
            "larpconnect.data.database.admin.username",
            "njall_admin",
            "larpconnect.data.database.admin.password",
            "njall_admin",
            "larpconnect.data.database.users.jdbc-url",
            jdbcUrl,
            "larpconnect.data.database.users.username",
            "njall_users",
            "larpconnect.data.database.users.password",
            "njall_users");
    return ConfigFactory.parseMap(configMap).withFallback(ConfigFactory.load());
  }
}
