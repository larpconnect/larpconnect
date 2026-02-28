package com.larpconnect.njall.common;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/** Default implementation of the {@link Clock} using the system clock. */
@Singleton
final class DefaultClock implements Clock {

  @Inject
  DefaultClock() {}

  @Override
  public long currentTimeMillis() {
    return System.currentTimeMillis();
  }

  @Override
  public long nanoTime() {
    return System.nanoTime();
  }
}
