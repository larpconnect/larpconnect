package com.larpconnect.njall.api.http;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.http.javadsl.model.HttpHeader;
import org.apache.pekko.http.javadsl.model.HttpRequest;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.http.javadsl.model.headers.RawHeader;
import org.apache.pekko.http.javadsl.server.Directives;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class TracingDirectiveTest {

  private static final Pattern W3C_PATTERN = Pattern.compile("^00-[0-9a-f]{32}-[0-9a-f]{16}-01$");
  private static final String FORGED_TRACEPARENT =
      "00-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa-bbbbbbbbbbbbbbbb-01";

  private static ActorSystem<Void> system;
  private TracingDirective tracingDirective;

  @BeforeAll
  static void setUpSystem() {
    system = ActorSystem.create(Behaviors.empty(), "tracing-directive-test");
  }

  @AfterAll
  static void tearDownSystem() throws Exception {
    system.terminate();
    system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  @BeforeEach
  void setUp() {
    var tracerProvider = SdkTracerProvider.builder().build();
    OpenTelemetry otel = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();
    Tracer tracer = otel.getTracer("com.larpconnect.njall.api.test");
    tracingDirective = new DefaultTracingDirective(tracer);
  }

  @Test
  @DisplayName("trace attaches compliant W3C traceparent header to response")
  void trace_inboundRequest_attachesTraceparentHeader() throws Exception {
    var route = tracingDirective.trace(() -> Directives.complete(StatusCodes.OK, "traced"));
    var handler = route.seal().function(system);

    var response =
        handler.apply(HttpRequest.GET("/test")).toCompletableFuture().get(5, TimeUnit.SECONDS);

    assertThat(response.status()).isEqualTo(StatusCodes.OK);
    var traceHeader = response.getHeader("traceparent");
    assertThat(traceHeader).isPresent();
    assertThat(traceHeader.get().value()).matches(W3C_PATTERN);
  }

  @Test
  @DisplayName("trace propagates generated traceparent header down to inner route request")
  void trace_inboundRequest_propagatesHeaderToInnerRoute() throws Exception {
    var capturedHeader = new AtomicReference<String>();
    var route =
        tracingDirective.trace(
            () ->
                Directives.extractRequest(
                    req -> {
                      capturedHeader.set(
                          req.getHeader("traceparent").map(HttpHeader::value).orElse(null));
                      return Directives.complete(StatusCodes.OK, "propagated");
                    }));
    var handler = route.seal().function(system);

    var response =
        handler.apply(HttpRequest.GET("/test")).toCompletableFuture().get(5, TimeUnit.SECONDS);

    assertThat(response.status()).isEqualTo(StatusCodes.OK);
    assertThat(capturedHeader.get()).isNotNull().matches(W3C_PATTERN);
  }

  @Test
  @DisplayName("trace ignores client-supplied traceparent header and generates new trace ID")
  void trace_forgedHeader_discardsAndGeneratesFreshTraceId() throws Exception {
    var route = tracingDirective.trace(() -> Directives.complete(StatusCodes.OK, "isolated"));
    var handler = route.seal().function(system);

    var request =
        HttpRequest.GET("/test").addHeader(RawHeader.create("traceparent", FORGED_TRACEPARENT));
    var response = handler.apply(request).toCompletableFuture().get(5, TimeUnit.SECONDS);

    var traceHeader = response.getHeader("traceparent");
    assertThat(traceHeader).isPresent();
    var headerValue = traceHeader.get().value();
    assertThat(headerValue).matches(W3C_PATTERN);
    assertThat(headerValue).doesNotContain("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
  }

  @Test
  @DisplayName("trace records exception and completes with 500 when inner supplier throws")
  void trace_innerThrows_recordsExceptionAndCompletesWith500() throws Exception {
    var route =
        tracingDirective.trace(
            () -> {
              throw new RuntimeException("Route failure");
            });
    var handler = route.seal().function(system);

    var response =
        handler.apply(HttpRequest.GET("/fail")).toCompletableFuture().get(5, TimeUnit.SECONDS);

    assertThat(response.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }
}
