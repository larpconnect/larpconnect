package com.larpconnect.njall.init;

import com.larpconnect.njall.common.annotations.AiContract;
import io.vertx.core.Verticle;

/** A central interface for managing the deployment of Verticles. */
public interface VerticleDeployer {

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
