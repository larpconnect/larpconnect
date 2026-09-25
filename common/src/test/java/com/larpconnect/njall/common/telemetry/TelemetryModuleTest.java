package com.larpconnect.njall.common.telemetry;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class TelemetryModuleTest {

  @Test
  @DisplayName("TelemetryModule configures OpenTelemetry and Tracer singletons")
  void configure_createsInjector_providesSingletons() {
    var injector = Guice.createInjector(new TelemetryModule());

    var otel1 = injector.getInstance(OpenTelemetry.class);
    var otel2 = injector.getInstance(OpenTelemetry.class);
    var tracer1 = injector.getInstance(Tracer.class);
    var tracer2 = injector.getInstance(Tracer.class);

    assertThat(otel1).isNotNull().isSameAs(otel2);
    assertThat(tracer1).isNotNull().isSameAs(tracer2);
  }

  @Test
  @DisplayName("Tracer starts and ends spans with valid OpenTelemetry context")
  void tracer_startSpan_producesValidSpanContext() {
    var injector = Guice.createInjector(new TelemetryModule());
    var tracer = injector.getInstance(Tracer.class);

    var span = tracer.spanBuilder("test-operation").startSpan();
    try {
      var spanContext = span.getSpanContext();
      assertThat(spanContext.isValid()).isTrue();
      assertThat(spanContext.getTraceId()).matches("^[0-9a-f]{32}$");
      assertThat(spanContext.getSpanId()).matches("^[0-9a-f]{16}$");

      var traceContext = new TraceContext(span);
      assertThat(traceContext.toTraceparent())
          .isEqualTo("00-" + spanContext.getTraceId() + "-" + spanContext.getSpanId() + "-01");
    } finally {
      span.end();
    }
  }
}
