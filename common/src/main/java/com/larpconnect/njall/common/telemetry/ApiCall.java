package com.larpconnect.njall.common.telemetry;

import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.annotations.ImmutableTypeParameter;
import java.util.Optional;

/**
 * Generic actor command and message envelope carrying correlation context.
 *
 * @param <T> The payload command type.
 * @param call The underlying command payload.
 * @param context Optional OpenTelemetry trace context.
 */
@Immutable
public record ApiCall<@ImmutableTypeParameter T>(T call, Optional<TraceContext> context) {

  public ApiCall(T call) {
    this(call, Optional.empty());
  }

  public ApiCall(T call, TraceContext context) {
    this(call, Optional.of(context));
  }
}
