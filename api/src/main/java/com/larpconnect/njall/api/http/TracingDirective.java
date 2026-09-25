package com.larpconnect.njall.api.http;

import java.util.function.Supplier;
import org.apache.pekko.http.javadsl.server.Route;

/** Directive establishing OpenTelemetry root server spans and response headers. */
public interface TracingDirective {

  /**
   * Intercepts incoming requests with OpenTelemetry root spans and injects traceparent headers.
   *
   * @param innerRouteSupplier the inner route supplier
   * @return the traced route
   */
  Route trace(Supplier<Route> innerRouteSupplier);
}
