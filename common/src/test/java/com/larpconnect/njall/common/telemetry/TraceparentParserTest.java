package com.larpconnect.njall.common.telemetry;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class TraceparentParserTest {

  private static final String VALID_TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
  private static final String VALID_SPAN_ID = "00f067aa0ba902b7";

  @Test
  @DisplayName("parse with valid header returns populated optional")
  void parse_validHeader_returnsTraceContext() {
    var rawHeader = "00-" + VALID_TRACE_ID + "-" + VALID_SPAN_ID + "-01";
    var result = TraceparentParser.parse(rawHeader);
    assertThat(result).isPresent();
    assertThat(result.get().traceId()).isEqualTo(VALID_TRACE_ID);
    assertThat(result.get().spanId()).isEqualTo(VALID_SPAN_ID);
  }

  @Test
  @DisplayName("parse with null or invalid headers returns empty optional")
  void parse_invalidHeaders_returnsEmpty() {
    assertThat(TraceparentParser.parse(null)).isEmpty();
    assertThat(TraceparentParser.parse("")).isEmpty();
    assertThat(TraceparentParser.parse("not-a-traceparent")).isEmpty();
    assertThat(TraceparentParser.parse("01-" + VALID_TRACE_ID + "-" + VALID_SPAN_ID + "-01"))
        .isEmpty();
    assertThat(TraceparentParser.parse("00-invalid-" + VALID_SPAN_ID + "-01")).isEmpty();
  }
}
