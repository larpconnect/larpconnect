package com.larpconnect.njall.common.time;

import com.google.common.util.concurrent.Service;

/**
 * A service interface extending {@link MonotonicClock} and {@link Service} to manage the lifecycle
 * of the temporal operations provider.
 *
 * <p>Following the Single Implementation Interface pattern, this interface bridges the base {@code
 * MonotonicClock} capability with Guava's state management. This ensures that the clock can be
 * properly started during Guice's synchronous initialization phase, guaranteeing the availability
 * of a stable clock source across the application before any asynchronous Vert.x components
 * execute.
 */
public interface TimeService extends MonotonicClock, Service {}
