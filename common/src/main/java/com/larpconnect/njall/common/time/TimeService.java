package com.larpconnect.njall.common.time;

import com.google.common.util.concurrent.Service;

/**
 * A service interface extending {@link MonotonicClock} and {@link Service} to manage the lifecycle
 * of the temporal operations provider.
 */
public interface TimeService extends MonotonicClock, Service {}
