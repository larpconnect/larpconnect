package com.larpconnect.njall.init;

import com.google.common.util.concurrent.Service;
import com.larpconnect.njall.common.annotations.AiContract;
import io.vertx.core.Verticle;

public interface VerticleService extends Service {
  @AiContract(
      require = "verticleClass \\neq \\bot",
      ensure = "verticleClass \\text{ is deployed}",
      implementationHint = "Deploys the specified verticle class to the Vert.x instance.")
  void deploy(Class<? extends Verticle> verticleClass);
}
