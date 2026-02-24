package com.larpconnect.njall.init;

import com.google.common.util.concurrent.Service;
import com.google.inject.Injector;
import io.vertx.core.Vertx;

public interface VerticleService extends Service {
  Injector getInjector();

  Vertx getVertx();
}
