package com.larpconnect.njall.init;

import static com.google.common.base.Preconditions.checkState;

import com.google.inject.Injector;
import com.larpconnect.njall.common.annotations.AiContract;
import com.larpconnect.njall.proto.Message;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import jakarta.inject.Inject;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class VerticleSetupService {
  private final Logger logger = LoggerFactory.getLogger(VerticleSetupService.class);
  private final AtomicReference<Vertx> vertxRef = new AtomicReference<>();

  @Inject
  VerticleSetupService(ProtoCodecRegistry protoCodecRegistry) {
    logger.info("Loaded ProtoCodecRegistry: {}", protoCodecRegistry);
  }

  @AiContract(
      require = {"vertx \neq \bot", "injector \neq \bot"},
      ensure = {
        "vertxRef \neq \bot",
        "guice: \text{ factory is registered}",
        "protobuf \text{ codec is registered}"
      },
      implementationHint = "Registers Guice verticle factory and Proto codec.")
  void setup(Vertx vertx, Injector injector) {
    vertx.registerVerticleFactory(new GuiceVerticleFactory(injector));
    vertx.eventBus().registerDefaultCodec(Message.class, new ProtoCodecRegistry());
    vertxRef.set(vertx);
  }

  @AiContract(
      require = {"verticleClass \neq \bot", "vertxRef \neq \bot"},
      ensure = "verticleClass \text{ is deployed}",
      implementationHint = "Deploys the verticle using the guice prefix.")
  void deploy(Class<? extends Verticle> verticleClass) {
    var vertx = vertxRef.get();
    checkState(vertx != null, "Vertx not initialized");
    vertx
        .deployVerticle("guice:" + verticleClass.getName())
        .onSuccess(
            id ->
                logger.info(
                    "{} deployed successfully with ID: {}", verticleClass.getSimpleName(), id))
        .onFailure(
            err -> {
              logger.error("Failed to deploy verticle", err);
              throw new RuntimeException(
                  "Failed to deploy verticle " + verticleClass.getName(), err);
            });
  }
}
