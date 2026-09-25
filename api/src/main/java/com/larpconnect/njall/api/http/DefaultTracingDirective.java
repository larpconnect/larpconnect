package com.larpconnect.njall.api.http;

import com.google.inject.Inject;
import com.larpconnect.njall.common.telemetry.TraceContext;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import java.util.function.Supplier;
import org.apache.pekko.http.javadsl.model.HttpRequest;
import org.apache.pekko.http.javadsl.model.headers.RawHeader;
import org.apache.pekko.http.javadsl.server.AllDirectives;
import org.apache.pekko.http.javadsl.server.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Default implementation of {@link TracingDirective} using OpenTelemetry. */
final class DefaultTracingDirective extends AllDirectives implements TracingDirective {

  private static final Logger logger = LoggerFactory.getLogger(DefaultTracingDirective.class);
  private static final String TRACEPARENT_HEADER = "traceparent";

  private final Tracer tracer;

  @Inject
  DefaultTracingDirective(Tracer tracer) {
    this.tracer = tracer;
  }

  @Override
  public Route trace(Supplier<Route> innerRouteSupplier) {
    return extractRequest(request -> traceRequest(request, innerRouteSupplier));
  }

  private Route traceRequest(HttpRequest request, Supplier<Route> innerRouteSupplier) {
    var span = createRootSpan(request);
    var traceContext = TraceContext.fromSpan(span);
    var header = RawHeader.create(TRACEPARENT_HEADER, traceContext.toTraceparent());
    var tracedRequest = request.removeHeader(TRACEPARENT_HEADER).addHeader(header);

    return respondWithHeader(
        header,
        () ->
            mapRequest(
                _ -> tracedRequest,
                () ->
                    mapResponse(
                        response -> {
                          span.end();
                          return response;
                        },
                        () -> executeInner(span, tracedRequest, innerRouteSupplier))));
  }

  private Route executeInner(Span span, HttpRequest request, Supplier<Route> innerRouteSupplier) {
    try (var _ = span.makeCurrent()) {
      logger.info("Processing HTTP {} {}", request.method().value(), request.getUri().path());
      return innerRouteSupplier.get();
    } catch (Throwable t) {
      span.recordException(t);
      span.end();
      throw t;
    }
  }

  private Span createRootSpan(HttpRequest request) {
    var spanName = "HTTP " + request.method().value() + " " + request.getUri().path();
    return tracer.spanBuilder(spanName).setSpanKind(SpanKind.SERVER).startSpan();
  }
}
