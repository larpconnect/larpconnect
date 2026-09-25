package com.larpconnect.njall.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Modules;
import com.larpconnect.njall.common.config.ServerConfig;
import com.larpconnect.njall.data.session.SessionFactoryFactory;
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
import org.hibernate.SessionFactory;

public final class RootEndpointSteps {

  private HttpServerService serverService;
  private ActorSystem<Void> system;
  private int boundPort;
  private HttpResponse<String> response;

  @Given("the HTTP server is running on an ephemeral port")
  public void theHttpServerIsRunningOnAnEphemeralPort() throws Exception {
    var mockSessionFactory = mock(SessionFactory.class);
    when(mockSessionFactory.isClosed()).thenReturn(false);
    var mockFactory = mock(SessionFactoryFactory.class);
    when(mockFactory.create(any(), any())).thenReturn(mockSessionFactory);

    var injector =
        Guice.createInjector(
            Modules.override(new ServerModule())
                .with(
                    new AbstractModule() {
                      @Override
                      protected void configure() {
                        // Bind to ephemeral port 0 for test isolation
                        bind(ServerConfig.class).toInstance(new ServerConfig("127.0.0.1", 0));
                        bind(SessionFactoryFactory.class).toInstance(mockFactory);
                      }
                    }));

    serverService = injector.getInstance(HttpServerService.class);
    system = injector.getInstance(Key.get(new TypeLiteral<ActorSystem<Void>>() {}));

    serverService.start().toCompletableFuture().get(10, TimeUnit.SECONDS);
    boundPort = serverService.getBoundPort();
    assertThat(boundPort).isPositive();
  }

  @When("the client sends a GET request to {string}")
  public void theClientSendsAGetRequestTo(String path) throws Exception {
    var client = HttpClient.newHttpClient();
    var request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + boundPort + path))
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build();

    response = client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  @Then("the response status code should be {int}")
  public void theResponseStatusCodeShouldBe(int expectedStatusCode) {
    assertThat(response.statusCode()).isEqualTo(expectedStatusCode);
  }

  @Then("the response body should be empty")
  public void theResponseBodyShouldBeEmpty() {
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
