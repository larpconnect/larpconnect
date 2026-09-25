package com.larpconnect.njall.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
import io.opentelemetry.instrumentation.logback.mdc.v1_0.OpenTelemetryAppender;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.apache.pekko.actor.typed.ActorSystem;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.NativeQuery;
import org.slf4j.LoggerFactory;

/** Cucumber step definitions for OpenTelemetry distributed tracing integration scenarios. */
public final class TelemetryTracingSteps {

  private static final Pattern W3C_TRACEPARENT_PATTERN =
      Pattern.compile("^00-([0-9a-f]{32})-([0-9a-f]{16})-01$");

  private HttpServerService serverService;
  private ActorSystem<Void> system;
  private int boundPort;
  private HttpResponse<String> response;
  private OpenTelemetryAppender otelAppender;
  private ListAppender<ILoggingEvent> listAppender;
  private Logger rootLogger;

  @Given("the HTTP server is running with telemetry enabled")
  public void theHttpServerIsRunningWithTelemetryEnabled() throws Exception {
    startServer(false);
  }

  @Given("the HTTP server is running with telemetry and log capture enabled")
  public void theHttpServerIsRunningWithTelemetryAndLogCaptureEnabled() throws Exception {
    startServer(true);
  }

  private void startServer(boolean captureLogs) throws Exception {
    if (captureLogs) {
      rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
      listAppender = new ListAppender<>();
      listAppender.start();
      otelAppender = new OpenTelemetryAppender();
      otelAppender.addAppender(listAppender);
      otelAppender.start();
      rootLogger.addAppender(otelAppender);
    }

    var mockSessionFactory = mock(SessionFactory.class);
    when(mockSessionFactory.isClosed()).thenReturn(false);
    var mockSession = mock(Session.class);
    @SuppressWarnings("unchecked")
    var mockQuery = (NativeQuery<Integer>) mock(NativeQuery.class);
    when(mockSessionFactory.openSession()).thenReturn(mockSession);
    when(mockSession.createNativeQuery(anyString(), org.mockito.ArgumentMatchers.eq(Integer.class)))
        .thenReturn(mockQuery);
    when(mockQuery.setTimeout(anyInt())).thenReturn(mockQuery);
    when(mockQuery.getSingleResult()).thenReturn(1);
    var mockFactory = mock(SessionFactoryFactory.class);
    when(mockFactory.create(any(), any())).thenReturn(mockSessionFactory);

    var injector =
        Guice.createInjector(
            Modules.override(new ServerModule())
                .with(
                    new AbstractModule() {
                      @Override
                      protected void configure() {
                        bind(ServerConfig.class).toInstance(ServerConfig.of("127.0.0.1", 0));
                        bind(SessionFactoryFactory.class).toInstance(mockFactory);
                      }
                    }));

    serverService = injector.getInstance(HttpServerService.class);
    system = injector.getInstance(Key.get(new TypeLiteral<ActorSystem<Void>>() {}));

    serverService.start().toCompletableFuture().get(10, TimeUnit.SECONDS);
    boundPort = serverService.getBoundPort();
    assertThat(boundPort).isPositive();
  }

  @When("a telemetry client sends a GET request to {string}")
  public void aTelemetryClientSendsAGetRequestTo(String path) throws Exception {
    var client = HttpClient.newHttpClient();
    var request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + boundPort + path))
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build();

    response = client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  @When("a telemetry client sends a GET request to {string} with traceparent header {string}")
  public void aTelemetryClientSendsAGetRequestWithHeader(String path, String headerValue)
      throws Exception {
    var client = HttpClient.newHttpClient();
    var request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + boundPort + path))
            .header("traceparent", headerValue)
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build();

    response = client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  @Then("the telemetry response status code should be {int}")
  public void theTelemetryResponseStatusCodeShouldBe(int expectedStatusCode) {
    assertThat(response.statusCode()).isEqualTo(expectedStatusCode);
  }

  @Then("the telemetry response should include a valid W3C traceparent header")
  public void theTelemetryResponseShouldIncludeAValidW3cTraceparentHeader() {
    var header = response.headers().firstValue("traceparent");
    assertThat(header).isPresent();
    assertThat(header.get()).matches(W3C_TRACEPARENT_PATTERN);
  }

  @Then("the telemetry response trace ID should not be {string}")
  public void theTelemetryResponseTraceIdShouldNotBe(String forgedTraceId) {
    var header = response.headers().firstValue("traceparent").orElseThrow();
    var matcher = W3C_TRACEPARENT_PATTERN.matcher(header);
    assertThat(matcher.matches()).isTrue();
    var traceId = matcher.group(1);
    assertThat(traceId).isNotEqualTo(forgedTraceId);
  }

  @Then("the captured server logs should contain the matching trace and span identifiers")
  public void theCapturedServerLogsShouldContainMatchingIdentifiers() {
    assertThat(listAppender).isNotNull();
    var header = response.headers().firstValue("traceparent").orElseThrow();
    var matcher = W3C_TRACEPARENT_PATTERN.matcher(header);
    assertThat(matcher.matches()).isTrue();
    var expectedTraceId = matcher.group(1);
    var expectedSpanId = matcher.group(2);

    var matchingEvent =
        listAppender.list.stream()
            .filter(
                event ->
                    expectedTraceId.equals(event.getMDCPropertyMap().get("trace_id"))
                        && expectedSpanId.equals(event.getMDCPropertyMap().get("span_id")))
            .findFirst();

    assertThat(matchingEvent)
        .as(
            "Expected at least one log event with trace_id=%s and span_id=%s",
            expectedTraceId, expectedSpanId)
        .isPresent();
  }

  @After
  public void tearDown() throws Exception {
    if (rootLogger != null && otelAppender != null) {
      rootLogger.detachAppender(otelAppender);
      otelAppender.stop();
      listAppender.stop();
      otelAppender = null;
      listAppender = null;
      rootLogger = null;
    }
    if (serverService != null) {
      serverService.stop().toCompletableFuture().get(5, TimeUnit.SECONDS);
      serverService = null;
    }
    if (system != null) {
      system.terminate();
      system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
      system = null;
    }
  }
}
