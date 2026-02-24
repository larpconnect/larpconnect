package com.larpconnect.init;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import io.vertx.core.Vertx;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Manages the lifecycle of the Vert.x application. */
public class VerticleLifecycle {
  private static final Logger logger = LoggerFactory.getLogger(VerticleLifecycle.class);
  private Vertx vertx;

  public VerticleLifecycle() {}

  /**
   * Starts the Vert.x application with the given Guice modules.
   *
   * @param modules the Guice modules to use
   */
  public void start(Module... modules) {
    logger.info("Starting Vert.x application");
    vertx = Vertx.vertx();

    // Create a module that binds the Vertx instance if needed, or other common bindings.
    // For now we assume modules passed in cover dependencies or we rely on JIT bindings.
    // However, GuiceVerticleFactory needs Injector.
    // We can bind Injector in a module if needed, but Guice automatically injects Injector.

    List<Module> allModules = new ArrayList<>(Arrays.asList(modules));
    // Add a module to bind Vertx instance?
    allModules.add(
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(Vertx.class).toInstance(vertx);
          }
        });

    Injector injector = Guice.createInjector(Stage.PRODUCTION, allModules);

    VerticleSetupService setupService = injector.getInstance(VerticleSetupService.class);
    setupService.setup(vertx);

    Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
  }

  /**
   * Deploys a verticle by name.
   *
   * @param name the name of the verticle to deploy
   */
  public void deployVerticle(String name) {
    if (vertx == null) {
      throw new IllegalStateException("Vertx not started");
    }
    logger.info("Deploying verticle: {}", name);
    vertx
        .deployVerticle(name)
        .onSuccess(id -> logger.info("Deployed {} with ID {}", name, id))
        .onFailure(err -> logger.error("Failed to deploy {}", name, err));
  }

  /** Stops the Vert.x application. */
  public void stop() {
    if (vertx != null) {
      logger.info("Stopping Vert.x application");
      vertx
          .close()
          .onSuccess(v -> logger.info("Vertx stopped successfully"))
          .onFailure(err -> logger.error("Failed to stop Vertx", err));
      vertx = null;
    }
  }
}
