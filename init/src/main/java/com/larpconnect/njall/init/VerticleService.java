package com.larpconnect.njall.init;

import com.google.common.util.concurrent.Service;
import com.larpconnect.njall.common.annotations.AiContract;
import io.vertx.core.Verticle;

/**
 * A central service interface for managing the initialization and deployment of Verticles.
 *
 * <p>The system relies heavily on Guice to satisfy dependencies. By extending {@link Service}, this
 * interface provides a clean lifecycle (via Guava's ServiceManager) to handle the complex
 * bootstrapping required to marry Vert.x's asynchronous deployment model with a synchronous,
 * injected setup process.
 */
public interface VerticleService extends Service {

  /**
   * Submits a Verticle class for deployment.
   *
   * @param verticleClass the class of the Verticle to deploy
   */
  @AiContract(
      require = "verticleClass \\neq \\bot",
      ensure = "verticleClass \\text{ is deployed}",
      implementationHint = "Deploys the specified verticle class to the Vert.x instance.")
  void deploy(Class<? extends Verticle> verticleClass);
}
