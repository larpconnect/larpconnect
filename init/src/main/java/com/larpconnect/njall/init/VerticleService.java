package com.larpconnect.njall.init;

import com.google.common.util.concurrent.Service;
import io.vertx.core.Verticle;

public interface VerticleService extends Service {
  void deploy(Class<? extends Verticle> verticleClass);
}
