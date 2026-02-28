package com.larpconnect.njall.common;

import com.larpconnect.njall.common.annotations.DefaultImplementation;

/** Provides access to system time. */
@DefaultImplementation(DefaultClock.class)
public interface Clock {

  /** Returns the current time in milliseconds. */
  long currentTimeMillis();

  /**
   * Returns the current value of the running Java Virtual Machine's high-resolution time source, in
   * nanoseconds.
   */
  long nanoTime();
}
