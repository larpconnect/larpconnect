package com.larpconnect.njall.common;

import com.github.benmanes.caffeine.cache.Ticker;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

/** Package-private binding module providing common utility instances. */
final class CommonBindingModule extends AbstractModule {

  @Provides
  Ticker provideTicker() {
    return Ticker.systemTicker();
  }
}
