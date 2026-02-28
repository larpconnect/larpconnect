package com.larpconnect.njall.common.time;

import com.google.common.util.concurrent.Service;
import com.larpconnect.njall.common.annotations.AiContract;

public interface TimeService extends Service {
  /**
   * Gets the monotonic time in milliseconds.
   *
   * @return the monotonic time in milliseconds
   */
  @AiContract(
      ensure = "$res \\ge 0",
      implementationHint = "Returns the current monotonic time in milliseconds.")
  long monotonicNowMillis();
}
