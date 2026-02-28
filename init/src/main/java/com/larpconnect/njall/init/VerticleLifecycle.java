package com.larpconnect.njall.init;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.larpconnect.njall.common.annotations.AiContract;
import com.larpconnect.njall.common.time.TimeService;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class VerticleLifecycle extends AbstractIdleService implements VerticleService {
  private static final int CONFIG_LOAD_TIMEOUT_SECONDS = 5;
  private final Logger logger = LoggerFactory.getLogger(VerticleLifecycle.class);

  private final ImmutableList<Module> modules;
  private final AtomicReference<VerticleSetupService> setupServiceRef = new AtomicReference<>();
  private final AtomicReference<Vertx> vertxRef = new AtomicReference<>();

  VerticleLifecycle(List<Module> modules) {
    this.modules = ImmutableList.copyOf(modules);
  }

  @Override
  @AiContract(
      ensure = {"setupServiceRef \\neq \\bot", "vertxRef.get() \\neq \\bot"},
      implementationHint =
          "Loads configuration, initializes Guice injector, and registers Vert.x factories.")
  protected void startUp() {
    logger.info("Starting VerticleLifecycle...");

    // Load default config
    JsonObject defaultConfig;
    try {
      var configResource = System.getProperty("njall.config.resource", "config.json");
      var url = Resources.getResource(configResource);
      defaultConfig = new JsonObject(Resources.toString(url, StandardCharsets.UTF_8));
    } catch (IllegalArgumentException | IOException e) {
      throw new RuntimeException("Failed to load default config", e);
    }

    // Create temp Vertx for config loading
    var tempVertx = Vertx.vertx();
    JsonObject config;
    try {
      var memoryStore = new ConfigStoreOptions().setType("json").setConfig(defaultConfig);
      var sysStore = new ConfigStoreOptions().setType("sys");
      var envStore = new ConfigStoreOptions().setType("env");

      var options =
          new ConfigRetrieverOptions().addStore(memoryStore).addStore(sysStore).addStore(envStore);
      var retriever = ConfigRetriever.create(tempVertx, options);
      var future = new CompletableFuture<JsonObject>();
      retriever.getConfig().onSuccess(future::complete).onFailure(future::completeExceptionally);

      // Use a timeout to prevent indefinite hanging in tests if vertx context is not running
      config = future.get(CONFIG_LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted while loading config", e);
    } catch (ExecutionException e) {
      throw new RuntimeException("Failed to load config", e);
    } catch (TimeoutException e) {
      throw new RuntimeException("Timed out loading config", e);
    } finally {
      tempVertx.close();
    }

    // Create a mutable list to add our internal modules
    var builder = ImmutableList.<Module>builder();
    builder.addAll(modules);
    builder.add(new VertxModule());
    builder.add(new ConfigModule(config));

    // Create Guice Injector
    var injector = Guice.createInjector(builder.build());

    var vertx = injector.getInstance(Vertx.class);
    vertxRef.set(vertx);

    // Setup Verticle Factory via VerticleSetupService
    var setupService = injector.getInstance(VerticleSetupService.class);
    setupService.setup(vertx, injector);
    setupServiceRef.set(setupService);

    // Start TimeService if bound
    try {
      var timeService = injector.getInstance(TimeService.class);
      timeService.startAsync().awaitRunning();
    } catch (com.google.inject.ConfigurationException e) {
      logger.debug("TimeService not bound, skipping start.");
    }

    logger.info("VerticleLifecycle started.");
  }

  @Override
  @AiContract(implementationHint = "Gracefully shuts down the Vert.x instance.")
  protected void shutDown() {
    logger.info("Stopping VerticleLifecycle...");
    var vertx = vertxRef.get();
    if (vertx != null) {
      var latch = new CountDownLatch(1);
      vertx
          .close()
          .onComplete(
              ar -> {
                if (ar.succeeded()) {
                  logger.info("Vert.x closed successfully.");
                } else {
                  logger.error("Failed to close Vert.x", ar.cause());
                }
                latch.countDown();
              });
      try {
        latch.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        logger.warn("Interrupted while waiting for Vert.x to close.");
      }
    }
  }

  @Override
  @AiContract(
      require = "verticleClass \\neq \\bot",
      ensure = "verticleClass \\text{ is deployed via setupService}",
      implementationHint = "Delegates deployment to the setup service.")
  public void deploy(Class<? extends Verticle> verticleClass) {
    var service = setupServiceRef.get();
    if (service != null) {
      service.deploy(verticleClass);
    } else {
      throw new IllegalStateException("VerticleLifecycle not started");
    }
  }
}
