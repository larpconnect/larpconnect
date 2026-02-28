package com.larpconnect.njall.common.time;

import com.google.common.util.concurrent.Service;

/** A service that provides access to the current time. */
public interface TimeService extends Service {

  /**
   * Returns the current time in milliseconds since the epoch.
   *
   * @return the current time in milliseconds
   */
  long currentTimeMillis();
}
