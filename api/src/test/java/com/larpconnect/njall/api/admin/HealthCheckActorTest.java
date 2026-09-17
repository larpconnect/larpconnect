package com.larpconnect.njall.api.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import org.apache.pekko.actor.testkit.typed.javadsl.BehaviorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class HealthCheckActorTest {

  private static Behavior<HealthCheckCommand> createBehavior(HealthCheckRegistry registry) {
    return Behaviors.setup(context -> new HealthCheckActor(context, registry));
  }

  @Test
  @DisplayName("HealthCheckActor responds with Healthy when all registry checks pass")
  void onCheckHealth_allHealthy_emitsHealthyResponse() {
    var registry = new HealthCheckRegistry();
    registry.register(
        "healthyCheck",
        new HealthCheck() {
          @Override
          protected Result check() {
            return Result.healthy();
          }
        });

    var testKit = BehaviorTestKit.create(createBehavior(registry));
    TestInbox<HealthCheckResponse> inbox = TestInbox.create();

    testKit.run(new HealthCheckCommand.CheckHealth(inbox.getRef()));

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
          protected Result check() {
            return Result.unhealthy("Disk full");
          }
        });

    var testKit = BehaviorTestKit.create(createBehavior(registry));
    TestInbox<HealthCheckResponse> inbox = TestInbox.create();

    testKit.run(new HealthCheckCommand.CheckHealth(inbox.getRef()));

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
          protected Result check() {
            return Result.unhealthy((String) null);
          }
        });

    var testKit = BehaviorTestKit.create(createBehavior(registry));
    TestInbox<HealthCheckResponse> inbox = TestInbox.create();

    testKit.run(new HealthCheckCommand.CheckHealth(inbox.getRef()));

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
          protected Result check() {
            return Result.unhealthy(exception);
          }
        });

    var testKit = BehaviorTestKit.create(createBehavior(registry));
    TestInbox<HealthCheckResponse> inbox = TestInbox.create();

    testKit.run(new HealthCheckCommand.CheckHealth(inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(HealthCheckResponse.Unhealthy.class);
    assertThat(((HealthCheckResponse.Unhealthy) response).reason())
        .isEqualTo("errorCheck: " + exception);
  }
}
