package com.larpconnect.njall.api.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.larpconnect.njall.common.telemetry.ApiCall;
import com.larpconnect.njall.common.telemetry.TraceContext;
import io.dropwizard.metrics5.health.HealthCheck;
import io.dropwizard.metrics5.health.HealthCheck.Result;
import io.dropwizard.metrics5.health.HealthCheckRegistry;
import org.apache.pekko.actor.testkit.typed.javadsl.BehaviorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.apache.pekko.actor.typed.Behavior;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

final class HealthCheckActorTest {

  private static Behavior<ApiCall<HealthCheckCommand>> createBehavior(
      HealthCheckRegistry registry) {
    return HealthCheckActor.create(registry);
  }

  @Test
  @DisplayName("HealthCheckActor responds with Healthy when all registry checks pass")
  void onCheckHealth_allHealthy_emitsHealthyResponse() {
    var registry = new HealthCheckRegistry();
    registry.register(
        "healthyCheck",
        new HealthCheck() {
          @Override
          public Result check() {
            return Result.healthy();
          }
        });

    var testKit = BehaviorTestKit.create(createBehavior(registry));
    TestInbox<HealthCheckResponse> inbox = TestInbox.create();

    testKit.run(new ApiCall<>(new HealthCheckCommand.CheckHealth(inbox.getRef())));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(HealthCheckResponse.Healthy.class);
  }

  @Test
  @DisplayName("HealthCheckActor responds with Unhealthy when a check fails with a message")
  void onCheckHealth_unhealthyWithMessage_emitsUnhealthyResponse() {
    var registry = new HealthCheckRegistry();
    registry.register(
        "failingCheck",
        new HealthCheck() {
          @Override
          public Result check() {
            return Result.unhealthy("Disk full");
          }
        });

    var testKit = BehaviorTestKit.create(createBehavior(registry));
    TestInbox<HealthCheckResponse> inbox = TestInbox.create();

    testKit.run(new ApiCall<>(new HealthCheckCommand.CheckHealth(inbox.getRef())));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(HealthCheckResponse.Unhealthy.class);
    assertThat(((HealthCheckResponse.Unhealthy) response).reason())
        .isEqualTo("failingCheck: Disk full");
  }

  @Test
  @DisplayName("HealthCheckActor responds with Unhealthy when a check fails with null message")
  void onCheckHealth_unhealthyWithNullMessage_emitsUnhealthyResponse() {
    var registry = new HealthCheckRegistry();
    registry.register(
        "noMsgCheck",
        new HealthCheck() {
          @Override
          public Result check() {
            return Result.unhealthy((String) null);
          }
        });

    var testKit = BehaviorTestKit.create(createBehavior(registry));
    TestInbox<HealthCheckResponse> inbox = TestInbox.create();

    testKit.run(new ApiCall<>(new HealthCheckCommand.CheckHealth(inbox.getRef())));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(HealthCheckResponse.Unhealthy.class);
    assertThat(((HealthCheckResponse.Unhealthy) response).reason())
        .isEqualTo("noMsgCheck: unhealthy");
  }

  @Test
  @DisplayName(
      "HealthCheckActor responds with Unhealthy containing error when check fails with exception")
  void onCheckHealth_unhealthyWithException_emitsUnhealthyResponse() {
    var registry = new HealthCheckRegistry();
    var exception = new IllegalStateException();
    registry.register(
        "errorCheck",
        new HealthCheck() {
          @Override
          public Result check() {
            return Result.unhealthy(exception);
          }
        });

    var testKit = BehaviorTestKit.create(createBehavior(registry));
    TestInbox<HealthCheckResponse> inbox = TestInbox.create();

    testKit.run(new ApiCall<>(new HealthCheckCommand.CheckHealth(inbox.getRef())));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(HealthCheckResponse.Unhealthy.class);
    assertThat(((HealthCheckResponse.Unhealthy) response).reason())
        .isEqualTo("errorCheck: " + exception);
  }

  @Test
  @DisplayName("HealthCheckActor preserves trace_id and span_id in MDC during execution")
  void onCheckHealth_withTraceContext_populatesMdcDuringExecution() {
    var registry = new HealthCheckRegistry();
    var expectedTraceId = "4bf92f3577b34da6a3ce929d0e0e4736";
    var expectedSpanId = "00f067aa0ba902b7";
    var traceContext = new TraceContext(expectedTraceId, expectedSpanId);

    registry.register(
        "mdcValidationCheck",
        new HealthCheck() {
          @Override
          public Result check() {
            var mdcTraceId = MDC.get("trace_id");
            var mdcSpanId = MDC.get("span_id");
            if (expectedTraceId.equals(mdcTraceId) && expectedSpanId.equals(mdcSpanId)) {
              return Result.healthy();
            }
            return Result.unhealthy(
                "MDC mismatch: trace_id=" + mdcTraceId + ", span_id=" + mdcSpanId);
          }
        });

    var testKit = BehaviorTestKit.create(createBehavior(registry));
    TestInbox<HealthCheckResponse> inbox = TestInbox.create();

    testKit.run(new ApiCall<>(new HealthCheckCommand.CheckHealth(inbox.getRef()), traceContext));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(HealthCheckResponse.Healthy.class);
  }

  @Test
  @DisplayName("extractMdc extracts trace_id and span_id when TraceContext is present")
  void extractMdc_withTraceContext_returnsPopulatedMap() {
    TestInbox<HealthCheckResponse> inbox = TestInbox.create();
    var traceContext = new TraceContext("4bf92f3577b34da6a3ce929d0e0e4736", "00f067aa0ba902b7");
    ApiCall<HealthCheckCommand> apiCall =
        new ApiCall<>(new HealthCheckCommand.CheckHealth(inbox.getRef()), traceContext);

    var mdc = HealthCheckActor.extractMdc(apiCall);

    assertThat(mdc)
        .containsEntry("trace_id", "4bf92f3577b34da6a3ce929d0e0e4736")
        .containsEntry("span_id", "00f067aa0ba902b7");
  }

  @Test
  @DisplayName("extractMdc returns empty map when TraceContext is empty")
  void extractMdc_withoutTraceContext_returnsEmptyMap() {
    TestInbox<HealthCheckResponse> inbox = TestInbox.create();
    ApiCall<HealthCheckCommand> apiCall =
        new ApiCall<>(new HealthCheckCommand.CheckHealth(inbox.getRef()));

    var mdc = HealthCheckActor.extractMdc(apiCall);

    assertThat(mdc).isEmpty();
  }
}
