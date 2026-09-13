package com.larpconnect.njall.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Modules;
import com.larpconnect.njall.common.config.ServerConfig;
import com.larpconnect.njall.server.ServerModule;
import com.larpconnect.njall.server.http.HttpServerService;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.typed.ActorSystem;

public final class HealthEndpointSteps {

  private HttpServerService serverService;
  private ActorSystem<Void> system;
  private int boundPort;
  private HttpResponse<String> response;

  @Given("the HTTP server is running and the Pekko framework is healthy")
  public void theHttpServerIsRunningAndPekkoIsHealthy() throws Exception {
    var injector =
        Guice.createInjector(
            Modules.override(new ServerModule())
                .with(
                    new AbstractModule() {
                      @Override
                      protected void configure() {
                        bind(ServerConfig.class).toInstance(ServerConfig.of("127.0.0.1", 0));
                      }
                    }));

    serverService = injector.getInstance(HttpServerService.class);
    system = injector.getInstance(Key.get(new TypeLiteral<ActorSystem<Void>>() {}));

    serverService.start().toCompletableFuture().get(10, TimeUnit.SECONDS);
    boundPort = serverService.getBoundPort();
    assertThat(boundPort).isPositive();
  }

  @When("a client sends an HTTP GET request to {string}")
  public void aClientSendsAnHttpGetRequestTo(String path) throws Exception {
    sendRequest(path);
  }

  @When("a client sends an HTTP GET request to {string} without authorization headers")
  public void aClientSendsAnHttpGetRequestWithoutAuth(String path) throws Exception {
    sendRequest(path);
  }

  private void sendRequest(String path) throws Exception {
    var client = HttpClient.newHttpClient();
    var request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + boundPort + path))
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build();

    response = client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  @Then("the health probe response status code should be {int}")
  public void theHealthProbeResponseStatusCodeShouldBe(int expectedStatusCode) {
    assertThat(response.statusCode()).isEqualTo(expectedStatusCode);
  }

  @Then("the health probe response body should be empty")
  public void theHealthProbeResponseBodyShouldBeEmpty() {
    assertThat(response.body()).isEmpty();
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
}
