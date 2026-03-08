package com.larpconnect.njall.common.time;

import com.google.common.util.concurrent.Service;

/**
 * A service interface extending {@link Time} and {@link Service} to manage the lifecycle of the
 * temporal operations provider.
 */
public interface TimeService extends Time, Service {}
