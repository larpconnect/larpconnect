package com.larpconnect.njall.common.telemetry;

import com.google.errorprone.annotations.Immutable;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import java.util.regex.Pattern;

/**
 * Immutable correlation context representing an active OpenTelemetry trace and span.
 *
 * @param traceId 32-character lowercase hexadecimal trace identifier
 * @param spanId 16-character lowercase hexadecimal span identifier
 */
@Immutable
public record TraceContext(String traceId, String spanId) {

  private static final Pattern TRACE_ID_PATTERN = Pattern.compile("^[0-9a-f]{32}$");
  private static final Pattern SPAN_ID_PATTERN = Pattern.compile("^[0-9a-f]{16}$");
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
   */
  public TraceContext(Span span) {
    this(span.getSpanContext());
  }

  /**
   * Constructs a {@link TraceContext} from an OpenTelemetry {@link SpanContext}.
   *
   * @param spanContext the span context containing trace and span IDs
   */
  public TraceContext(SpanContext spanContext) {
    this(spanContext.getTraceId(), spanContext.getSpanId());
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
