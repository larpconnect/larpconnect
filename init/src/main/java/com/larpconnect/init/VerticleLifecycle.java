package com.larpconnect.init;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import io.vertx.core.Vertx;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Manages the lifecycle of the Vert.x application. */
public final class VerticleLifecycle extends AbstractIdleService {
  private static final Logger logger = LoggerFactory.getLogger(VerticleLifecycle.class);
  private final AtomicReference<Vertx> vertxRef = new AtomicReference<>();
  private final Module[] modules;
  private final Runtime runtime;

  VerticleLifecycle(Runtime runtime, Module... modules) {
    this.runtime = runtime;
    this.modules = modules;
  }

  // Factory method for creating VerticleLifecycle
  public static VerticleLifecycle create(Module... modules) {
    return new VerticleLifecycle(Runtime.getRuntime(), modules);
  }

  @Override
  protected void startUp() {
    logger.info("Starting Vert.x application");
    Vertx vertx = Vertx.vertx();
    if (!vertxRef.compareAndSet(null, vertx)) {
      vertx.close(); // Should not happen if startUp is called once
      throw new IllegalStateException("Vertx already started");
    }

    List<Module> allModules = new ArrayList<>(Arrays.asList(modules));
    allModules.add(new InitModule(vertx));

    Injector injector = Guice.createInjector(Stage.PRODUCTION, allModules);

    VerticleSetupService setupService = injector.getInstance(VerticleSetupService.class);
    setupService.setup(vertx);

    // Register shutdown hook
    runtime.addShutdownHook(new Thread(this::stopAsync));
  }

  @Override
  protected void shutDown() {
    Vertx vertx = vertxRef.getAndSet(null);
    if (vertx != null) {
      logger.info("Stopping Vert.x application");
      vertx
          .close()
          .onSuccess(v -> logger.info("Vertx stopped successfully"))
          .onFailure(err -> logger.error("Failed to stop Vertx", err));
    }
  }

  /**
   * Deploys a verticle by name.
   *
   * @param name the name of the verticle to deploy
   */
  public void deployVerticle(String name) {
    Vertx vertx = vertxRef.get();
    if (vertx == null) {
      throw new IllegalStateException("Vertx not started");
    }
    logger.info("Deploying verticle: {}", name);
    vertx
        .deployVerticle(name)
        .onSuccess(id -> logger.info("Deployed {} with ID {}", name, id))
        .onFailure(err -> logger.error("Failed to deploy {}", name, err));
  }

  /** Guice module for internal bindings. */
  private static final class InitModule extends AbstractModule {
    private final Vertx vertx;

    InitModule(Vertx vertx) {
      this.vertx = vertx;
    }

    @Override
    protected void configure() {
      bind(Vertx.class).toInstance(vertx);
      bind(ProtoCodecRegistry.class).to(DefaultProtoCodecRegistry.class);
      bind(VerticleSetupService.class).to(DefaultVerticleSetupService.class);
    }
  }
}
