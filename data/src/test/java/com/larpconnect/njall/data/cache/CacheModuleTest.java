package com.larpconnect.njall.data.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.github.benmanes.caffeine.cache.Ticker;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.larpconnect.njall.data.dao.studios.StudioLookupDAO;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class CacheModuleTest {

  @Test
  @DisplayName("CacheModule binds StudioLookupCache and StudioLookupCacheService as singleton")
  void configure_whenInjected_providesCacheServiceSingleton() {
    var mockStudioLookupDao = mock(StudioLookupDAO.class);
    var config =
        ConfigFactory.parseString("larpconnect.data.cache.studio-lookup.refresh-interval = 5m");
    var mockDependenciesModule =
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(StudioLookupDAO.class).toInstance(mockStudioLookupDao);
            bind(Config.class).toInstance(config);
            bind(Ticker.class).toInstance(Ticker.systemTicker());
          }
        };

    var injector = Guice.createInjector(mockDependenciesModule, new CacheModule());

    var cache = injector.getInstance(StudioLookupCache.class);
    var service = injector.getInstance(StudioLookupCacheService.class);

    assertThat(cache).isInstanceOf(DefaultStudioLookupCacheService.class);
    assertThat(service).isInstanceOf(DefaultStudioLookupCacheService.class);
    assertThat(cache).isSameAs(service);
  }
}
