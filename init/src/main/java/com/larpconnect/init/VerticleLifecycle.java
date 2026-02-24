package com.larpconnect.init;

import com.google.common.util.concurrent.AbstractIdleService;
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
  private final AtomicReference<VertxProvider> vertxProviderRef = new AtomicReference<>();
  private final Module[] modules;

  VerticleLifecycle(Module... modules) {
    this.modules = modules;
  }

  // Factory method for creating VerticleLifecycle
  public static VerticleLifecycle create(Module... modules) {
    return new VerticleLifecycle(modules);
  }

  @Override
  protected void startUp() {
    logger.info("Starting Vert.x application");

    List<Module> allModules = new ArrayList<>(Arrays.asList(modules));
    allModules.add(new InitModule());

    Injector injector = Guice.createInjector(Stage.PRODUCTION, allModules);

    VertxProvider vertxProvider = injector.getInstance(VertxProvider.class);
    if (!vertxProviderRef.compareAndSet(null, vertxProvider)) {
      throw new IllegalStateException("Lifecycle already started");
    }

    VerticleSetupService setupService = injector.getInstance(VerticleSetupService.class);
    // This will trigger Vertx creation via VertxProvider
    setupService.setup(vertxProvider.get());
  }

  @Override
  protected void shutDown() {
    VertxProvider provider = vertxProviderRef.getAndSet(null);
    if (provider != null) {
      Vertx vertx = provider.getIfCreated();
      if (vertx != null) {
        logger.info("Stopping Vert.x application");
        vertx
            .close()
            .onSuccess(v -> logger.info("Vertx stopped successfully"))
            .onFailure(err -> logger.error("Failed to stop Vertx", err));
      }
    }
  }

  /**
   * Deploys a verticle by name.
   *
   * @param name the name of the verticle to deploy
   */
  public void deployVerticle(String name) {
    VertxProvider provider = vertxProviderRef.get();
    if (provider == null) {
      throw new IllegalStateException("Vertx not started");
    }
    Vertx vertx = provider.get();
    logger.info("Deploying verticle: {}", name);
    vertx
        .deployVerticle(name)
        .onSuccess(id -> logger.info("Deployed {} with ID {}", name, id))
        .onFailure(err -> logger.error("Failed to deploy {}", name, err));
  }
}
