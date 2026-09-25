package com.larpconnect.njall.common.telemetry;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;

/** Guice module providing OpenTelemetry SDK and Tracer singletons for Project Njall. */
public final class TelemetryModule extends AbstractModule {

  private static final String INSTRUMENTATION_SCOPE_NAME = "com.larpconnect.njall";

  @Override
  protected void configure() {
    // Guice bindings configured via provider methods
  }

  @Provides
  @Singleton
  OpenTelemetry provideOpenTelemetry() {
    return buildOpenTelemetrySdk();
  }

  @Provides
  @Singleton
  Tracer provideTracer(OpenTelemetry openTelemetry) {
    return openTelemetry.getTracer(INSTRUMENTATION_SCOPE_NAME);
  }

  private OpenTelemetry buildOpenTelemetrySdk() {
    var tracerProvider = SdkTracerProvider.builder().build();
    var propagators = ContextPropagators.create(W3CTraceContextPropagator.getInstance());
    return OpenTelemetrySdk.builder()
        .setTracerProvider(tracerProvider)
        .setPropagators(propagators)
        .build();
  }
}
