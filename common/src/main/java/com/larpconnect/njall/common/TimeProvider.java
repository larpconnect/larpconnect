package com.larpconnect.njall.common;

/** Provides access to system time. */
public interface TimeProvider {

  /** Returns the current time in milliseconds. */
  long currentTimeMillis();

  /**
   * Returns the current value of the running Java Virtual Machine's high-resolution time source, in
   * nanoseconds.
   */
  long nanoTime();
}
