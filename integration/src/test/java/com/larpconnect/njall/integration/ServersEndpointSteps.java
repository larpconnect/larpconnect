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
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.typed.ActorSystem;

/** Cucumber step definitions for administrative servers endpoint integration scenarios. */
public final class ServersEndpointSteps {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private HttpServerService serverService;
  private ActorSystem<Void> system;
  private int boundPort;
  private HttpResponse<String> response;
  private JsonNode rootJson;

  @Given("the HTTP server is running with database migrations applied and seed records present")
  public void theHttpServerIsRunningWithDatabaseMigrationsAppliedAndSeedRecordsPresent()
      throws Exception {
    DatabaseMigrationSteps.ensureStartedAndMigrated();

    var config = buildIntegrationConfig(DatabaseMigrationSteps.getJdbcUrl());
    var injector = Guice.createInjector(new ServerModule(config));

    serverService = injector.getInstance(HttpServerService.class);
    system = injector.getInstance(Key.get(new TypeLiteral<ActorSystem<Void>>() {}));

    serverService.start().toCompletableFuture().get(10, TimeUnit.SECONDS);
    boundPort = serverService.getBoundPort();
    assertThat(boundPort).isPositive();
  }

  @When("an admin client sends a GET request to {string}")
  public void anAdminClientSendsAGetRequestTo(String path) throws Exception {
    var client = HttpClient.newHttpClient();
    var request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + boundPort + path))
            .timeout(Duration.ofSeconds(20))
            .GET()
            .build();

    response = client.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.body() != null && !response.body().isBlank()) {
      rootJson = MAPPER.readTree(response.body());
    }
  }

  @Then("the admin response status code should be {int}")
  public void theAdminResponseStatusCodeShouldBe(int expectedStatusCode) {
    assertThat(response.statusCode()).isEqualTo(expectedStatusCode);
  }

  @Then("the admin response content-type should be {string}")
  public void theAdminResponseContentTypeShouldBe(String expectedContentType) {
    var contentType = response.headers().firstValue("Content-Type").orElse("");
    assertThat(contentType).contains(expectedContentType);
  }

  @Then(
      "the response body contains a JSON array of servers with fields {string}, {string},"
          + " {string}, {string}, and an array of {string}")
  public void theResponseBodyContainsAJsonArrayOfServersWithFieldsAndContacts(
      String f1, String f2, String f3, String f4, String f5) {
    assertThat(rootJson).isNotNull();
    assertThat(rootJson.isArray()).isTrue();
    assertThat(rootJson.size()).isGreaterThanOrEqualTo(1);

    for (var serverNode : rootJson) {
      assertThat(serverNode.hasNonNull(f1)).isTrue();
      assertThat(serverNode.hasNonNull(f2)).isTrue();
      assertThat(serverNode.hasNonNull(f3)).isTrue();
      assertThat(serverNode.hasNonNull(f4)).isTrue();
      assertThat(serverNode.has(f5)).isTrue();
      assertThat(serverNode.get(f5).isArray()).isTrue();
    }
  }

  @Then("each contact contains {string}, {string}, {string}, {string}, and {string}")
  public void eachContactContainsFields(String c1, String c2, String c3, String c4, String c5) {
    var totalContacts = 0;
    for (var serverNode : rootJson) {
      var contacts = serverNode.get("contacts");
      for (var contactNode : contacts) {
        assertThat(contactNode.hasNonNull(c1)).isTrue();
        assertThat(contactNode.hasNonNull(c2)).isTrue();
        assertThat(contactNode.hasNonNull(c3)).isTrue();
        assertThat(contactNode.hasNonNull(c4)).isTrue();
        assertThat(contactNode.hasNonNull(c5)).isTrue();
        totalContacts++;
      }
    }
    assertThat(totalContacts).isGreaterThanOrEqualTo(1);
  }

  @Then("the server list contains a server named {string} with primary domain {string}")
  public void theServerListContainsAServerNamedWithPrimaryDomain(
      String serverName, String primaryDomain) {
    var found = false;
    for (var serverNode : rootJson) {
      if (serverName.equals(serverNode.get("name").asText())
          && primaryDomain.equals(serverNode.get("primaryDomain").asText())) {
        found = true;
        break;
      }
    }
    assertThat(found).isTrue();
  }

  @Then("the server contacts contain an {string} {string} contact with value {string}")
  public void theServerContactsContainAnContactWithValue(
      String roleType, String contactType, String contactValue) {
    var found = false;
    for (var serverNode : rootJson) {
      for (var contactNode : serverNode.get("contacts")) {
        if (roleType.equals(contactNode.get("roleType").asText())
            && contactType.equals(contactNode.get("contactType").asText())
            && contactValue.equals(contactNode.get("contact").asText())) {
          found = true;
          break;
        }
      }
    }
    assertThat(found).isTrue();
  }

  @After
  public void tearDown() throws Exception {
    if (serverService != null) {
      serverService.stop().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }
    if (system != null) {
      system.terminate();
      system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
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
            DatabaseMigrationSteps.getPasswordFor("njall_admin"),
            "larpconnect.data.database.users.jdbc-url",
            jdbcUrl,
            "larpconnect.data.database.users.username",
            "njall_users",
            "larpconnect.data.database.users.password",
            DatabaseMigrationSteps.getPasswordFor("njall_users"));
    return ConfigFactory.parseMap(configMap).withFallback(ConfigFactory.load());
  }
}
