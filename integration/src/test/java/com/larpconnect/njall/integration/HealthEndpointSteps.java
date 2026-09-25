package com.larpconnect.njall.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.util.Modules;
import com.larpconnect.njall.api.admin.HealthCheckActorFactory;
import com.larpconnect.njall.common.config.ServerConfig;
import com.larpconnect.njall.data.session.SessionFactoryFactory;
import com.larpconnect.njall.server.ServerModule;
import com.larpconnect.njall.server.http.HttpServerService;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.dropwizard.metrics5.health.HealthCheck;
import io.dropwizard.metrics5.health.HealthCheck.Result;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.NativeQuery;

public final class HealthEndpointSteps {

  private HttpServerService serverService;
  private ActorSystem<Void> system;
  private SessionFactory mockSessionFactory;
  private int boundPort;
  private HttpResponse<String> response;

  @Given("the HTTP server is running and the Pekko framework is healthy")
  public void theHttpServerIsRunningAndPekkoIsHealthy() throws Exception {
    startServer();
  }

  @Given("the Pekko framework is terminating or an unhealthy state is detected")
  public void thePekkoFrameworkIsTerminatingOrAnUnhealthyStateIsDetected() throws Exception {
    startServer(
        new AbstractModule() {
          @Override
          protected void configure() {
            Multibinder.newSetBinder(binder(), HealthCheck.class)
                .addBinding()
                .toInstance(
                    new HealthCheck() {
                      @Override
                      public Result check() {
                        return Result.unhealthy("Subsystem unhealthy");
                      }
                    });
          }
        });
  }

  @Given("the health check actor does not respond within the configured ask timeout")
  public void theHealthCheckActorDoesNotRespondWithinTheConfiguredAskTimeout() throws Exception {
    startServer(
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(HealthCheckActorFactory.class)
                .toInstance(() -> Behaviors.receiveMessage(msg -> Behaviors.same()));
          }
        });
  }

  @Given(
      "the njall_admin database connection fails, is terminated, or times out during probe"
          + " evaluation")
  public void theNjallAdminDatabaseConnectionFails() throws Exception {
    startServer(
        new AbstractModule() {
          @Override
          protected void configure() {
            var failingSessionFactory = mock(SessionFactory.class);
            when(failingSessionFactory.isClosed()).thenReturn(false);
            when(failingSessionFactory.openSession())
                .thenThrow(new RuntimeException("Database unreachable"));
            var failingFactory = mock(SessionFactoryFactory.class);
            when(failingFactory.create(any(), any())).thenReturn(failingSessionFactory);
            bind(SessionFactoryFactory.class).toInstance(failingFactory);
          }
        });
  }

  @SuppressWarnings("unchecked")
  private void startServer(Module... extraModules) throws Exception {
    mockSessionFactory = mock(SessionFactory.class);
    when(mockSessionFactory.isClosed()).thenReturn(false);

    var mockSession = mock(Session.class);
    var mockQuery = (NativeQuery<Integer>) mock(NativeQuery.class);
    when(mockSessionFactory.openSession()).thenReturn(mockSession);
    when(mockSession.createNativeQuery(anyString(), org.mockito.ArgumentMatchers.eq(Integer.class)))
        .thenReturn(mockQuery);
    when(mockQuery.setTimeout(anyInt())).thenReturn(mockQuery);
    when(mockQuery.getSingleResult()).thenReturn(1);

    var mockFactory = mock(SessionFactoryFactory.class);
    when(mockFactory.create(any(), any())).thenReturn(mockSessionFactory);

    var baseOverride =
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(ServerConfig.class).toInstance(new ServerConfig("127.0.0.1", 0));
            bind(SessionFactoryFactory.class).toInstance(mockFactory);
          }
        };

    var overrideModule =
        extraModules.length > 0 ? Modules.override(baseOverride).with(extraModules) : baseOverride;

    var injector = Guice.createInjector(Modules.override(new ServerModule()).with(overrideModule));

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

  @When("a client sends another HTTP GET request to {string} within the cache window")
  public void aClientSendsAnotherHttpGetRequestWithinCacheWindow(String path) throws Exception {
    sendRequest(path);
  }

  @Then("no new database ping query is executed")
  public void noNewDatabasePingQueryIsExecuted() {
    verify(mockSessionFactory, times(1)).openSession();
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
