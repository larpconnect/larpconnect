package com.larpconnect.njall.init;

import com.google.common.util.concurrent.Service;

/**
 * A central service interface for managing the initialization and deployment of Verticles.
 *
 * <p>The system relies heavily on Guice to satisfy dependencies. By extending {@link Service}, this
 * interface provides a clean lifecycle (via Guava's ServiceManager) to handle the complex
 * bootstrapping required to marry Vert.x's asynchronous deployment model with a synchronous,
 * injected setup process.
 */
public interface VerticleService extends VerticleDeployer, Service {}
