package com.larpconnect.njall.data.cache;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/** Guice module binding in-memory cache services for data layer components. */
public final class CacheModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(DefaultStudioLookupCacheService.class).in(Scopes.SINGLETON);
    bind(StudioLookupCache.class).to(DefaultStudioLookupCacheService.class);
    bind(StudioLookupCacheService.class).to(DefaultStudioLookupCacheService.class);
  }
}
