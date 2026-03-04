package com.larpconnect.njall.init;

import static com.larpconnect.njall.common.annotations.ContractTag.PURE;

import com.google.inject.Module;
import com.larpconnect.njall.common.annotations.AiContract;
import io.vertx.core.Vertx;
import io.vertx.core.spi.VerticleFactory;
import java.util.List;

public interface VerticleServices {

  /** Creates a new instance of {@link VerticleService} using the provided Vertx and factory. */
  static VerticleService create(Vertx vertx, VerticleFactory verticleFactory) {
    return new VerticleLifecycle(vertx, verticleFactory);
  }

  @AiContract(
      require = "modules \\neq \\bot",
      ensure = {"$res \\neq \\bot", "$res \\text{ is a new VerticleLifecycle}"},
      tags = PURE,
      implementationHint = "Creates a new VerticleService instance with the provided modules.")
  static VerticleService create(List<Module> modules) {
    return new VerticleLifecycle(modules);
  }
}
