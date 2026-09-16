package com.larpconnect.njall.server;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.larpconnect.njall.common.annotation.Blocking;
import com.larpconnect.njall.data.session.ActiveSessionFactories;
import java.util.concurrent.CompletableFuture;
import org.apache.pekko.Done;
import org.apache.pekko.actor.CoordinatedShutdown;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.DispatcherSelector;
import org.apache.pekko.actor.typed.Props;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

final class ServerBindingModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ServerManager.class).to(ServerManagerService.class);
    bind(ServerManagerService.class).to(DefaultServerManagerService.class).in(Scopes.SINGLETON);
    bind(ShutdownHookRegistrar.class).toInstance(createShutdownHookRegistrar());
  }

  private static ShutdownHookRegistrar createShutdownHookRegistrar() {
    return new RuntimeShutdownHookRegistrar();
  }

  private static final class RuntimeShutdownHookRegistrar implements ShutdownHookRegistrar {
    @Override
    public void registerShutdownHook(Thread hook) {
      Runtime.getRuntime().addShutdownHook(hook);
    }

    @Override
    public boolean removeShutdownHook(Thread hook) {
      try {
        return Runtime.getRuntime().removeShutdownHook(hook);
      } catch (IllegalStateException ignored) {
        return false;
      }
    }
  }

  @Provides
  @Singleton
  ActorSystem<Void> provideActorSystem(ActiveSessionFactories activeFactories) {
    ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "njall-server");
    CoordinatedShutdown.get(system)
        .addTask(
            CoordinatedShutdown.PhaseServiceStop(),
            "close-hibernate-session-factories",
            () ->
                CompletableFuture.supplyAsync(
                    () -> {
                      activeFactories.closeAll();
                      return Done.done();
                    }));
    return system;
  }

  @Provides
  @Blocking
  Props provideBlockingDispatcher() {
    return DispatcherSelector.fromConfig("larpconnect.blocking-dispatcher");
  }
}
