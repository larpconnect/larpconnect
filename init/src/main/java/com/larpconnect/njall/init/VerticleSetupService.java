package com.larpconnect.njall.init;

import static com.google.common.base.Preconditions.checkState;

import com.google.inject.Injector;
import com.larpconnect.njall.common.annotations.AiContract;
import com.larpconnect.njall.common.codec.ProtoCodec;
import com.larpconnect.njall.proto.MessageRequest;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
final class VerticleSetupService {
  private final Logger logger = LoggerFactory.getLogger(VerticleSetupService.class);
  private final AtomicReference<Vertx> vertxRef = new AtomicReference<>();
  private final ProtoCodec protoCodec;

  @Inject
  VerticleSetupService(ProtoCodec protoCodec) {
    this.protoCodec = protoCodec;
    logger.info("Loaded ProtoCodec: {}", protoCodec);
  }

  @AiContract(
      require = {"vertx \\neq \\bot", "injector \\neq \\bot"},
      ensure = {
        "vertxRef \\neq \\bot",
        "guice: \\text{ factory is registered}",
        "protobuf \\text{ codec is registered}"
      },
      implementationHint = "Registers Guice verticle factory and Proto codec.")
  void setup(Vertx vertx, Injector injector) {
    vertx.registerVerticleFactory(new GuiceVerticleFactory(injector));
    vertx.eventBus().registerDefaultCodec(MessageRequest.class, protoCodec);
    vertxRef.set(vertx);
  }

  @AiContract(
      require = {"verticleClass \\neq \\bot", "vertxRef \\neq \\bot"},
      ensure = "verticleClass \\text{ is deployed}",
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
              throw new IllegalStateException(
                  "Failed to deploy verticle " + verticleClass.getName(), err);
            });
  }
}
