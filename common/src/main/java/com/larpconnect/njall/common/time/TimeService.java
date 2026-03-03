package com.larpconnect.njall.common.time;

import com.google.common.util.concurrent.Service;
import com.larpconnect.njall.common.annotations.AiContract;

/**
 * A central service providing a uniform view of temporal operations within the application.
 *
 * <p>Directly referencing {@code System.currentTimeMillis()} or {@code System.nanoTime()} scatters
 * implicit, uncontrollable dependencies throughout the codebase. By injecting a uniform {@link
 * TimeService}, the application gains full control over the clock. This makes it possible to mock
 * time for unit tests, safely perform time-travel or freeze time scenarios, and guarantee
 * reproducibility in time-sensitive workflows.
 */
public interface TimeService extends Service {
  /**
   * Retrieves a reliable, monotonically increasing timestamp.
   *
   * <p>This method abstracts the operating system's monotonic clock. Monotonic clocks are immune to
   * backwards leaps, leap-seconds, or manual resets of the system clock. It should be strictly used
   * for tracking durations or elapsed time, rather than representing an exact wall-clock moment.
   *
   * @return a non-decreasing timestamp in milliseconds
   */
  @AiContract(
      ensure = "$res \\ge 0",
      implementationHint = "Returns the current monotonic time in milliseconds.")
  long monotonicNowMillis();
}
