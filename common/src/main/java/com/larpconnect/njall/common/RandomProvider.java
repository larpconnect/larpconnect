package com.larpconnect.njall.common;

/** Provides access to random numbers. */
public interface RandomProvider {

  /** Returns a pseudorandom long value. */
  long nextLong();

  /** Returns a pseudorandom int value between origin (inclusive) and bound (exclusive). */
  int nextInt(int origin, int bound);
}
