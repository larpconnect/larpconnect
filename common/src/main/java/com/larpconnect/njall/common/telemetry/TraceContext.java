package com.larpconnect.njall.common.telemetry;

import static java.util.Objects.requireNonNull;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import java.util.Optional;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Immutable correlation context representing an active OpenTelemetry trace and span.
 *
 * @param traceId 32-character lowercase hexadecimal trace identifier
 * @param spanId 16-character lowercase hexadecimal span identifier
 */
public record TraceContext(String traceId, String spanId) {

  private static final Pattern TRACE_ID_PATTERN = Pattern.compile("^[0-9a-f]{32}$");
  private static final Pattern SPAN_ID_PATTERN = Pattern.compile("^[0-9a-f]{16}$");
  private static final Pattern W3C_TRACEPARENT_PATTERN =
      Pattern.compile("^00-([0-9a-f]{32})-([0-9a-f]{16})-01$");
  private static final String TRACEPARENT_PREFIX = "00-";
  private static final String TRACEPARENT_SUFFIX = "-01";

  public TraceContext {
    if (!TRACE_ID_PATTERN.matcher(traceId).matches()) {
      throw new IllegalArgumentException("Invalid traceId format: " + traceId);
    }
    if (!SPAN_ID_PATTERN.matcher(spanId).matches()) {
      throw new IllegalArgumentException("Invalid spanId format: " + spanId);
    }
  }

  /**
   * Constructs a {@link TraceContext} from an active OpenTelemetry {@link Span}.
   *
   * @param span the span to extract identifiers from
   * @return the trace context
   */
  public static TraceContext fromSpan(Span span) {
    requireNonNull(span, "span cannot be null");
    return fromSpanContext(span.getSpanContext());
  }

  /**
   * Constructs a {@link TraceContext} from an OpenTelemetry {@link SpanContext}.
   *
   * @param spanContext the span context containing trace and span IDs
   * @return the trace context
   */
  public static TraceContext fromSpanContext(SpanContext spanContext) {
    requireNonNull(spanContext, "spanContext cannot be null");
    return new TraceContext(spanContext.getTraceId(), spanContext.getSpanId());
  }

  /**
   * Parses a W3C {@code traceparent} header string into a {@link TraceContext}.
   *
   * @param headerValue the raw header value
   * @return optional containing the parsed trace context, or empty if null or invalid
   */
  public static Optional<TraceContext> parseTraceparent(@Nullable String headerValue) {
    if (headerValue == null) {
      return Optional.empty();
    }
    var matcher = W3C_TRACEPARENT_PATTERN.matcher(headerValue);
    if (!matcher.matches()) {
      return Optional.empty();
    }
    return Optional.of(new TraceContext(matcher.group(1), matcher.group(2)));
  }

  /**
   * Formats this context as a standard W3C {@code traceparent} header value.
   *
   * @return the W3C traceparent header string
   */
  public String toTraceparent() {
    return TRACEPARENT_PREFIX + traceId + "-" + spanId + TRACEPARENT_SUFFIX;
  }
}
