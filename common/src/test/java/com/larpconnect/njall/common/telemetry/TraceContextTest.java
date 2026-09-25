package com.larpconnect.njall.common.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class TraceContextTest {

  private static final String VALID_TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
  private static final String VALID_SPAN_ID = "00f067aa0ba902b7";
  private static final Pattern W3C_PATTERN = Pattern.compile("^00-[0-9a-f]{32}-[0-9a-f]{16}-01$");

  @Test
  @DisplayName("Constructor accepts valid 32-hex traceId and 16-hex spanId")
  void constructor_validIdentifiers_createsInstance() {
    var context = new TraceContext(VALID_TRACE_ID, VALID_SPAN_ID);

    assertThat(context.traceId()).isEqualTo(VALID_TRACE_ID);
    assertThat(context.spanId()).isEqualTo(VALID_SPAN_ID);
  }

  @Test
  @DisplayName("toTraceparent formats standard W3C header value")
  void toTraceparent_validIdentifiers_matchesW3cPattern() {
    var context = new TraceContext(VALID_TRACE_ID, VALID_SPAN_ID);
    var header = context.toTraceparent();

    assertThat(header).isEqualTo("00-" + VALID_TRACE_ID + "-" + VALID_SPAN_ID + "-01");
    assertThat(W3C_PATTERN.matcher(header).matches()).isTrue();
  }

  @Test
  @DisplayName("fromSpanContext extracts traceId and spanId")
  void fromSpanContext_validSpanContext_returnsTraceContext() {
    var spanContext =
        SpanContext.create(
            VALID_TRACE_ID, VALID_SPAN_ID, TraceFlags.getSampled(), TraceState.getDefault());

    var context = TraceContext.fromSpanContext(spanContext);

    assertThat(context.traceId()).isEqualTo(VALID_TRACE_ID);
    assertThat(context.spanId()).isEqualTo(VALID_SPAN_ID);
  }

  @Test
  @DisplayName("fromSpan extracts identifiers from active span")
  void fromSpan_validSpan_returnsTraceContext() {
    var spanContext =
        SpanContext.create(
            VALID_TRACE_ID, VALID_SPAN_ID, TraceFlags.getSampled(), TraceState.getDefault());
    var span = Span.wrap(spanContext);

    var context = TraceContext.fromSpan(span);

    assertThat(context.traceId()).isEqualTo(VALID_TRACE_ID);
    assertThat(context.spanId()).isEqualTo(VALID_SPAN_ID);
  }

  @Test
  @DisplayName("Constructor rejects invalid traceId formats with IllegalArgumentException")
  void constructor_invalidTraceId_throwsIllegalArgumentException() {
    assertThatThrownBy(() -> new TraceContext("short", VALID_SPAN_ID))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid traceId format");

    assertThatThrownBy(() -> new TraceContext("4BF92F3577B34DA6A3CE929D0E0E4736", VALID_SPAN_ID))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid traceId format");
  }

  @Test
  @DisplayName("Constructor rejects invalid spanId formats with IllegalArgumentException")
  void constructor_invalidSpanId_throwsIllegalArgumentException() {
    assertThatThrownBy(() -> new TraceContext(VALID_TRACE_ID, "short"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid spanId format");

    assertThatThrownBy(() -> new TraceContext(VALID_TRACE_ID, "00F067AA0BA902B7"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid spanId format");
  }

  @Test
  @DisplayName("parseTraceparent parses valid W3C traceparent header string")
  void parseTraceparent_validHeader_returnsTraceContext() {
    var rawHeader = "00-" + VALID_TRACE_ID + "-" + VALID_SPAN_ID + "-01";
    var result = TraceContext.parseTraceparent(rawHeader);

    assertThat(result).isPresent();
    assertThat(result.get().traceId()).isEqualTo(VALID_TRACE_ID);
    assertThat(result.get().spanId()).isEqualTo(VALID_SPAN_ID);
  }

  @Test
  @DisplayName("parseTraceparent returns empty for null or invalid headers")
  void parseTraceparent_nullOrInvalid_returnsEmpty() {
    assertThat(TraceContext.parseTraceparent(null)).isEmpty();
    assertThat(TraceContext.parseTraceparent("")).isEmpty();
    assertThat(TraceContext.parseTraceparent("not-a-traceparent")).isEmpty();
    assertThat(TraceContext.parseTraceparent("01-" + VALID_TRACE_ID + "-" + VALID_SPAN_ID + "-01"))
        .isEmpty();
    assertThat(TraceContext.parseTraceparent("00-invalid-" + VALID_SPAN_ID + "-01")).isEmpty();
  }
}
