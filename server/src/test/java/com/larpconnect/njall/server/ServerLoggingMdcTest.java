package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.logback.mdc.v1_0.OpenTelemetryAppender;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

final class ServerLoggingMdcTest {

  private Logger rootLogger;
  private OpenTelemetryAppender otelAppender;
  private ListAppender<ILoggingEvent> listAppender;
  private Tracer tracer;

  @BeforeEach
  void setUp() {
    rootLogger = (Logger) LoggerFactory.getLogger(ServerLoggingMdcTest.class);
    rootLogger.setLevel(Level.INFO);

    listAppender = new ListAppender<>();
    listAppender.start();

    otelAppender = new OpenTelemetryAppender();
    otelAppender.addAppender(listAppender);
    otelAppender.start();

    rootLogger.addAppender(otelAppender);

    var tracerProvider = SdkTracerProvider.builder().build();
    OpenTelemetry openTelemetry =
        OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();
    tracer = openTelemetry.getTracer("com.larpconnect.njall.server.test");
  }

  @AfterEach
  void tearDown() {
    rootLogger.detachAppender(otelAppender);
    otelAppender.stop();
    listAppender.stop();
  }

  @Test
  @DisplayName("OpenTelemetryAppender injects trace_id and span_id into MDC during active span")
  void logging_withActiveSpan_injectsMdcIdentifiers() {
    Span span = tracer.spanBuilder("test-request").startSpan();
    try (var _ = span.makeCurrent()) {
      rootLogger.info("Executing traced operation");
    } finally {
      span.end();
    }

    assertThat(listAppender.list).hasSize(1);
    var event = listAppender.list.getFirst();
    var mdc = event.getMDCPropertyMap();

    assertThat(mdc).containsKey("trace_id");
    assertThat(mdc).containsKey("span_id");
    assertThat(mdc.get("trace_id")).isEqualTo(span.getSpanContext().getTraceId());
    assertThat(mdc.get("span_id")).isEqualTo(span.getSpanContext().getSpanId());
  }

  @Test
  @DisplayName("OpenTelemetryAppender does not inject trace_id when no span is active")
  void logging_withoutActiveSpan_doesNotInjectTraceId() {
    rootLogger.info("Executing untraced operation");

    assertThat(listAppender.list).hasSize(1);
    var event = listAppender.list.getFirst();
    var mdc = event.getMDCPropertyMap();

    assertThat(mdc.get("trace_id")).isNull();
  }
}
