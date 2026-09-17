package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.Service;
import com.larpconnect.njall.server.http.HttpServerService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.pekko.Done;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.http.javadsl.ServerBinding;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class ServerManagerServiceTest {

  @Test
  @DisplayName("startUp registers shutdown hook thread and starts HTTP server")
  void startUp_registersShutdownHookAndStartsServer_success() throws Exception {
    var registrar = new CapturingHookRegistrar();
    ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "test-sys-3");
    var httpService = mock(HttpServerService.class);
    var binding = mock(ServerBinding.class);

    when(httpService.start()).thenReturn(CompletableFuture.completedFuture(binding));
    when(httpService.stop()).thenReturn(CompletableFuture.completedFuture(Done.done()));

    var service = new DefaultServerManagerService(registrar, system, httpService);
    try {
      service.startAsync().awaitRunning(5, TimeUnit.SECONDS);

      assertThat(registrar.registered.get()).isNotNull();
      assertThat(registrar.registered.get().getName()).isEqualTo("pekko-coordinated-shutdown");
      verify(httpService).start();
    } finally {
      service.stopAsync().awaitTerminated(5, TimeUnit.SECONDS);
      assertThat(registrar.removed.get()).isSameAs(registrar.registered.get());
      terminateSystem(system);
    }
  }

  @Test
  @DisplayName("startUp fails and transitions service to FAILED when HTTP server start fails")
  void startUp_whenHttpServerStartFails_serviceEntersFailedState() {
    var registrar = mock(ShutdownHookRegistrar.class);
    ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "test-sys-4");
    var httpService = mock(HttpServerService.class);

    when(httpService.start())
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("port bound")));

    var service = new DefaultServerManagerService(registrar, system, httpService);
    try {
      assertThatIllegalStateException()
          .isThrownBy(() -> service.startAsync().awaitRunning(5, TimeUnit.SECONDS));
    } finally {
      terminateSystem(system);
    }
  }

  @Test
  @DisplayName("shutDown stops HTTP server and coordinates system shutdown")
  void shutDown_runningService_stopsHttpServerAndCoordinatesShutdown() throws Exception {
    var registrar = mock(ShutdownHookRegistrar.class);
    ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "test-sys-5");
    var httpService = mock(HttpServerService.class);
    var binding = mock(ServerBinding.class);

    when(httpService.start()).thenReturn(CompletableFuture.completedFuture(binding));
    when(httpService.stop()).thenReturn(CompletableFuture.completedFuture(Done.done()));

    var service = new DefaultServerManagerService(registrar, system, httpService);
    service.startAsync().awaitRunning(5, TimeUnit.SECONDS);
    service.stopAsync().awaitTerminated(5, TimeUnit.SECONDS);

    verify(httpService).stop();
    verify(registrar).removeShutdownHook(any(Thread.class));
    terminateSystem(system);
  }

  @Test
  @DisplayName("shutDown logs warning and continues when HTTP server stop fails")
  void shutDown_whenHttpStopFails_continuesShutdown() throws Exception {
    var registrar = mock(ShutdownHookRegistrar.class);
    ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "test-sys-6");
    var httpService = mock(HttpServerService.class);
    var binding = mock(ServerBinding.class);

    when(httpService.start()).thenReturn(CompletableFuture.completedFuture(binding));
    when(httpService.stop())
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("stop failed")));

    var service = new DefaultServerManagerService(registrar, system, httpService);
    service.startAsync().awaitRunning(5, TimeUnit.SECONDS);
    service.stopAsync().awaitTerminated(5, TimeUnit.SECONDS);

    verify(httpService).stop();
    verify(registrar).removeShutdownHook(any(Thread.class));
    terminateSystem(system);
  }

  @Test
  @DisplayName("shutDown continues and coordinates shutdown when removing shutdown hook fails")
  void shutDown_whenRemoveShutdownHookFails_continuesShutdown() throws Exception {
    var registrar = mock(ShutdownHookRegistrar.class);
    doThrow(new IllegalStateException("JVM already terminating"))
        .when(registrar)
        .removeShutdownHook(any(Thread.class));
    ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "test-sys-hook-fail");
    var httpService = mock(HttpServerService.class);
    var binding = mock(ServerBinding.class);

    when(httpService.start()).thenReturn(CompletableFuture.completedFuture(binding));
    when(httpService.stop()).thenReturn(CompletableFuture.completedFuture(Done.done()));

    var service = new DefaultServerManagerService(registrar, system, httpService);
    service.startAsync().awaitRunning(5, TimeUnit.SECONDS);
    service.stopAsync().awaitTerminated(5, TimeUnit.SECONDS);

    verify(httpService).stop();
    verify(registrar).removeShutdownHook(any(Thread.class));
    terminateSystem(system);
  }

  @Test
  @DisplayName("shutdownThread runs onJvmShutdown and stops service successfully")
  void shutdownThread_whenRun_stopsService() throws Exception {
    var registrar = new CapturingHookRegistrar();
    ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "test-sys-7");
    var httpService = mock(HttpServerService.class);
    var binding = mock(ServerBinding.class);

    when(httpService.start()).thenReturn(CompletableFuture.completedFuture(binding));
    when(httpService.stop()).thenReturn(CompletableFuture.completedFuture(Done.done()));

    var service = new DefaultServerManagerService(registrar, system, httpService);
    try {
      service.startAsync().awaitRunning(5, TimeUnit.SECONDS);
      var hook = registrar.registered.get();
      assertThat(hook).isNotNull();

      hook.start();
      hook.join(5000);

      assertThat(service.state()).isEqualTo(Service.State.TERMINATED);
    } finally {
      terminateSystem(system);
    }
  }

  @Test
  @DisplayName("shutdownThread logs warning and does not propagate exception when stop throws")
  void shutdownThread_whenStopFails_doesNotThrow() throws Exception {
    var registrar = mock(ShutdownHookRegistrar.class);
    ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "test-sys-8");
    var httpService = mock(HttpServerService.class);

    when(httpService.start())
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("forced start failure")));

    var service = new DefaultServerManagerService(registrar, system, httpService);
    try {
      assertThatIllegalStateException()
          .isThrownBy(() -> service.startAsync().awaitRunning(5, TimeUnit.SECONDS));

      assertThat(service.state()).isEqualTo(Service.State.FAILED);

      var hook = service.createShutdownThread();
      hook.start();
      hook.join(5000);

      assertThat(hook.isAlive()).isFalse();
    } finally {
      terminateSystem(system);
    }
  }

  private static void terminateSystem(ActorSystem<Void> system) {
    if (system != null) {
      try {
        system.terminate();
        system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
      } catch (Exception ignored) {
        // Ignored in test cleanup
      }
    }
  }

  private static final class CapturingHookRegistrar implements ShutdownHookRegistrar {
    private final AtomicReference<Thread> registered = new AtomicReference<>();
    private final AtomicReference<Thread> removed = new AtomicReference<>();

    @Override
    public void registerShutdownHook(Thread hook) {
      registered.set(hook);
    }

    @Override
    public boolean removeShutdownHook(Thread hook) {
      removed.set(hook);
      return true;
    }
  }
}
