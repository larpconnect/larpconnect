package com.larpconnect.njall.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.benmanes.caffeine.cache.Ticker;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.larpconnect.njall.common.annotation.Blocking;
import com.larpconnect.njall.common.config.ServerConfig;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class CommonModuleTest {

  @Test
  @DisplayName(
      "configure installs submodules and resolves ServerConfig, Ticker, Clock, and Executor")
  void configure_createsInjector_resolvesServerConfig() throws Exception {
    var injector = Guice.createInjector(new CommonModule());
    var serverConfig = injector.getInstance(ServerConfig.class);
    var ticker = injector.getInstance(Ticker.class);
    var clock = injector.getInstance(Clock.class);
    var blockingExecutor = injector.getInstance(Key.get(Executor.class, Blocking.class));

    assertThat(serverConfig.host()).isEqualTo("0.0.0.0");
    assertThat(serverConfig.port()).isEqualTo(8080);
    assertThat(ticker).isSameAs(Ticker.systemTicker());
    assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
    assertThat(injector.getInstance(Key.get(Executor.class, Blocking.class)))
        .isSameAs(blockingExecutor);

    boolean ranOnVirtualThread =
        CompletableFuture.supplyAsync(() -> Thread.currentThread().isVirtual(), blockingExecutor)
            .get(2, TimeUnit.SECONDS);
    assertThat(ranOnVirtualThread).isTrue();
  }
}
