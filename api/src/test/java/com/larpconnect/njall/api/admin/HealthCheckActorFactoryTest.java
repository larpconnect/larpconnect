package com.larpconnect.njall.api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.codahale.metrics.health.HealthCheckRegistry;
import org.apache.pekko.actor.testkit.typed.javadsl.BehaviorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class HealthCheckActorFactoryTest {

  @Test
  @DisplayName("DefaultHealthCheckActorFactory constructor throws NPE when registry is null")
  void constructor_nullRegistry_throwsNullPointerException() {
    assertThatNullPointerException()
        .isThrownBy(() -> new DefaultHealthCheckActorFactory(null))
        .withMessage("registry must not be null");
  }

  @Test
  @DisplayName("DefaultHealthCheckActorFactory.create produces executable behavior")
  void create_producesExecutableBehavior() {
    var registry = new HealthCheckRegistry();
    var factory = new DefaultHealthCheckActorFactory(registry);
    var behavior = factory.create();

    assertThat(behavior).isNotNull();

    var testKit = BehaviorTestKit.create(behavior);
    TestInbox<HealthCheckResponse> inbox = TestInbox.create();
    testKit.run(new HealthCheckCommand.CheckHealth(inbox.getRef()));

    assertThat(inbox.receiveMessage()).isInstanceOf(HealthCheckResponse.Healthy.class);
  }
}
