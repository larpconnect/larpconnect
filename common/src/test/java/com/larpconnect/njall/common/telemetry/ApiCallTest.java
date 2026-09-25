package com.larpconnect.njall.common.telemetry;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class ApiCallTest {

  private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
  private static final String SPAN_ID = "00f067aa0ba902b7";

  @Test
  @DisplayName("ApiCall constructor with empty context")
  void constructor_withoutContext_hasEmptyContext() {
    var call = new ApiCall<>("test-command");
    assertThat(call.call()).isEqualTo("test-command");
    assertThat(call.context()).isEmpty();
  }

  @Test
  @DisplayName("ApiCall constructor with direct TraceContext")
  void constructor_withTraceContext_populatesContext() {
    var traceContext = new TraceContext(TRACE_ID, SPAN_ID);
    var call = new ApiCall<>("test-command", traceContext);
    assertThat(call.call()).isEqualTo("test-command");
    assertThat(call.context()).contains(traceContext);
  }

  @Test
  @DisplayName("ApiCall canonical constructor with Optional")
  void canonicalConstructor_withOptional_populatesContext() {
    var traceContext = new TraceContext(TRACE_ID, SPAN_ID);
    var call = new ApiCall<>("test-command", Optional.of(traceContext));
    assertThat(call.call()).isEqualTo("test-command");
    assertThat(call.context()).contains(traceContext);
  }
}
