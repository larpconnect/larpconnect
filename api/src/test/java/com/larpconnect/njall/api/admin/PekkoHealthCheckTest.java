package com.larpconnect.njall.api.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.CoordinatedShutdown;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class PekkoHealthCheckTest {

  @Test
  @DisplayName("PekkoHealthCheck returns healthy when ActorSystem is running")
  void check_runningSystem_returnsHealthy() throws Exception {
    ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "pekko-health-test-running");
    try {
      var healthCheck = new PekkoHealthCheck(system);
      var result = healthCheck.check();

      assertThat(result.isHealthy()).isTrue();
    } finally {
      system.terminate();
      system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }
  }

  @Test
  @DisplayName("PekkoHealthCheck returns unhealthy when ActorSystem is terminated")
  void check_terminatedSystem_returnsUnhealthy() throws Exception {
    ActorSystem<Void> system =
        ActorSystem.create(Behaviors.empty(), "pekko-health-test-terminated");
    system.terminate();
    system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);

    var healthCheck = new PekkoHealthCheck(system);
    var result = healthCheck.check();

    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getMessage()).contains("ActorSystem is terminated");
  }

  @Test
  @DisplayName("PekkoHealthCheck returns unhealthy when CoordinatedShutdown is initiated")
  void check_coordinatedShutdownInitiated_returnsUnhealthy() throws Exception {
    ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "pekko-health-test-shutdown");
    try {
      CoordinatedShutdown.get(system).run(CoordinatedShutdown.jvmExitReason());
      var healthCheck = new PekkoHealthCheck(system);
      var result = healthCheck.check();

      assertThat(result.isHealthy()).isFalse();
      assertThat(result.getMessage()).contains("coordinated shutdown initiated");
    } finally {
      system.terminate();
      system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }
  }
}
