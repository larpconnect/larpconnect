package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.Service;
import com.larpconnect.njall.server.http.HttpServerService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
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
  @DisplayName("startUp registers shutdown hook thread, prewarms cache, and starts HTTP server")
  void startUp_registersShutdownHookAndStartsServer_success() throws Exception {
    var registrar = new CapturingHookRegistrar();
    ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "test-sys-3");
    var httpService = mock(HttpServerService.class);
    var events = new CopyOnWriteArrayList<String>();
    var cacheService = new TestStudioLookupCacheService(events);
    var binding = mock(ServerBinding.class);

    when(httpService.start())
        .thenAnswer(
            inv -> {
              events.add("http.start");
              return CompletableFuture.completedFuture(binding);
            });
    when(httpService.stop())
        .thenAnswer(
            inv -> {
              events.add("http.stop");
              return CompletableFuture.completedFuture(Done.done());
            });

    var service = new DefaultServerManagerService(registrar, system, httpService, cacheService);
    try {
      service.startAsync().awaitRunning(5, TimeUnit.SECONDS);

      assertThat(registrar.registered.get()).isNotNull();
      assertThat(registrar.registered.get().getName()).isEqualTo("pekko-coordinated-shutdown");
      assertThat(events).containsExactly("cache.startUp", "http.start");
    } finally {
      service.stopAsync().awaitTerminated(5, TimeUnit.SECONDS);
      assertThat(registrar.removed.get()).isSameAs(registrar.registered.get());
      assertThat(events)
          .containsExactly("cache.startUp", "http.start", "http.stop", "cache.shutDown");
      terminateSystem(system);
    }
  }

  @Test
  @DisplayName("startUp fails before starting HTTP server when cache service start fails")
  void startUp_whenCacheServiceStartFails_serviceEntersFailedState() {
    var registrar = mock(ShutdownHookRegistrar.class);
    ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "test-sys-cache-fail");
    var httpService = mock(HttpServerService.class);
    var cacheService = TestStudioLookupCacheService.failingOnStart();

    var service = new DefaultServerManagerService(registrar, system, httpService, cacheService);
    try {
      assertThatIllegalStateException()
          .isThrownBy(() -> service.startAsync().awaitRunning(5, TimeUnit.SECONDS));

      verify(httpService, never()).start();
    } finally {
      terminateSystem(system);
    }
  }

  @Test
  @DisplayName("startUp fails and transitions service to FAILED when HTTP server start fails")
  void startUp_whenHttpServerStartFails_serviceEntersFailedState() {
    var registrar = mock(ShutdownHookRegistrar.class);
    ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "test-sys-4");
    var httpService = mock(HttpServerService.class);
    var cacheService = new TestStudioLookupCacheService();

    when(httpService.start())
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("port bound")));

    var service = new DefaultServerManagerService(registrar, system, httpService, cacheService);
    try {
      assertThatIllegalStateException()
          .isThrownBy(() -> service.startAsync().awaitRunning(5, TimeUnit.SECONDS));
    } finally {
      terminateSystem(system);
    }
  }

  @Test
  @DisplayName("shutDown stops HTTP server and cache service then coordinates system shutdown")
  void shutDown_runningService_stopsHttpServerAndCoordinatesShutdown() throws Exception {
    var registrar = mock(ShutdownHookRegistrar.class);
    ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "test-sys-5");
    var httpService = mock(HttpServerService.class);
    var events = new CopyOnWriteArrayList<String>();
    var cacheService = new TestStudioLookupCacheService(events);
    var binding = mock(ServerBinding.class);

    when(httpService.start()).thenReturn(CompletableFuture.completedFuture(binding));
    when(httpService.stop())
        .thenAnswer(
            inv -> {
              events.add("http.stop");
              return CompletableFuture.completedFuture(Done.done());
            });

    var service = new DefaultServerManagerService(registrar, system, httpService, cacheService);
    service.startAsync().awaitRunning(5, TimeUnit.SECONDS);
    service.stopAsync().awaitTerminated(5, TimeUnit.SECONDS);

    assertThat(events).containsSubsequence("http.stop", "cache.shutDown");
    assertThat(cacheService.state()).isEqualTo(Service.State.TERMINATED);
    verify(registrar).removeShutdownHook(any(Thread.class));
    terminateSystem(system);
  }

  @Test
  @DisplayName("shutDown logs warning and continues when HTTP server stop fails")
  void shutDown_whenHttpStopFails_continuesShutdown() throws Exception {
    var registrar = mock(ShutdownHookRegistrar.class);
    ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "test-sys-6");
    var httpService = mock(HttpServerService.class);
    var cacheService = new TestStudioLookupCacheService();
    var binding = mock(ServerBinding.class);

    when(httpService.start()).thenReturn(CompletableFuture.completedFuture(binding));
    when(httpService.stop())
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("stop failed")));

    var service = new DefaultServerManagerService(registrar, system, httpService, cacheService);
    service.startAsync().awaitRunning(5, TimeUnit.SECONDS);
    service.stopAsync().awaitTerminated(5, TimeUnit.SECONDS);

    verify(httpService).stop();
    assertThat(cacheService.state()).isEqualTo(Service.State.TERMINATED);
    verify(registrar).removeShutdownHook(any(Thread.class));
    terminateSystem(system);
  }

  @Test
  @DisplayName("shutDown logs warning and continues when cache service stop fails")
  void shutDown_whenCacheStopFails_continuesShutdown() throws Exception {
    var registrar = mock(ShutdownHookRegistrar.class);
    ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "test-sys-cache-stop-fail");
    var httpService = mock(HttpServerService.class);
    var cacheService = TestStudioLookupCacheService.failingOnStop();
    var binding = mock(ServerBinding.class);

    when(httpService.start()).thenReturn(CompletableFuture.completedFuture(binding));
    when(httpService.stop()).thenReturn(CompletableFuture.completedFuture(Done.done()));

    var service = new DefaultServerManagerService(registrar, system, httpService, cacheService);
    service.startAsync().awaitRunning(5, TimeUnit.SECONDS);
    service.stopAsync().awaitTerminated(5, TimeUnit.SECONDS);

    verify(httpService).stop();
    assertThat(cacheService.state()).isEqualTo(Service.State.FAILED);
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
    var cacheService = new TestStudioLookupCacheService();
    var binding = mock(ServerBinding.class);

    when(httpService.start()).thenReturn(CompletableFuture.completedFuture(binding));
    when(httpService.stop()).thenReturn(CompletableFuture.completedFuture(Done.done()));

    var service = new DefaultServerManagerService(registrar, system, httpService, cacheService);
    service.startAsync().awaitRunning(5, TimeUnit.SECONDS);
    service.stopAsync().awaitTerminated(5, TimeUnit.SECONDS);

    verify(httpService).stop();
    assertThat(cacheService.state()).isEqualTo(Service.State.TERMINATED);
    verify(registrar).removeShutdownHook(any(Thread.class));
    terminateSystem(system);
  }

  @Test
  @DisplayName("shutdownThread runs onJvmShutdown and stops service successfully")
  void shutdownThread_whenRun_stopsService() throws Exception {
    var registrar = new CapturingHookRegistrar();
    ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "test-sys-7");
    var httpService = mock(HttpServerService.class);
    var cacheService = new TestStudioLookupCacheService();
    var binding = mock(ServerBinding.class);

    when(httpService.start()).thenReturn(CompletableFuture.completedFuture(binding));
    when(httpService.stop()).thenReturn(CompletableFuture.completedFuture(Done.done()));

    var service = new DefaultServerManagerService(registrar, system, httpService, cacheService);
    try {
      service.startAsync().awaitRunning(5, TimeUnit.SECONDS);
      var hook = registrar.registered.get();
      assertThat(hook).isNotNull();

      hook.start();
      hook.join(5000);

      assertThat(service.state()).isEqualTo(Service.State.TERMINATED);
      assertThat(cacheService.state()).isEqualTo(Service.State.TERMINATED);
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
    var cacheService = new TestStudioLookupCacheService();

    when(httpService.start())
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("forced start failure")));

    var service = new DefaultServerManagerService(registrar, system, httpService, cacheService);
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
    try {
      system.terminate();
      system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
    } catch (Exception ignored) {
      // Ignored in test cleanup
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
