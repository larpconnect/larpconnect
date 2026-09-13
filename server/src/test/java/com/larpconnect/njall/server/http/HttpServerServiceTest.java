package com.larpconnect.njall.server.http;

import static org.assertj.core.api.Assertions.assertThat;

import com.larpconnect.njall.api.http.RootRoute;
import com.larpconnect.njall.common.config.ServerConfig;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.Done;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.http.javadsl.server.AllDirectives;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class HttpServerServiceTest extends AllDirectives {

  private ActorSystem<Void> system;
  private HttpServerService serverService;

  @BeforeEach
  void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "test-server-lifecycle");
    RootRoute testRoute = () -> pathEndOrSingleSlash(() -> get(() -> complete(StatusCodes.OK, "")));
    serverService =
        new DefaultHttpServerService(system, ServerConfig.of("127.0.0.1", 0), testRoute);
  }

  @AfterEach
  void tearDown() throws Exception {
    serverService.stop().toCompletableFuture().get(5, TimeUnit.SECONDS);
    system.terminate();
    system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  @Test
  @DisplayName("start binds to ephemeral port and stop unbinds successfully")
  void start_dynamicPort_bindsAndUnbindsSuccessfully() throws Exception {
    var binding = serverService.start().toCompletableFuture().get(10, TimeUnit.SECONDS);

    assertThat(binding).isNotNull();
    assertThat(serverService.getBoundPort()).isPositive();

    var done = serverService.stop().toCompletableFuture().get(10, TimeUnit.SECONDS);
    assertThat(done).isEqualTo(Done.done());
    assertThat(serverService.getBoundPort()).isZero();
  }

  @Test
  @DisplayName("stop when not running returns completed future")
  void stop_notRunning_returnsDoneImmediately() throws Exception {
    var done = serverService.stop().toCompletableFuture().get(5, TimeUnit.SECONDS);

    assertThat(done).isEqualTo(Done.done());
  }

  @Test
  @DisplayName("getBoundPort returns zero when not running")
  void getBoundPort_notStarted_returnsZero() {
    assertThat(serverService.getBoundPort()).isZero();
  }
}
