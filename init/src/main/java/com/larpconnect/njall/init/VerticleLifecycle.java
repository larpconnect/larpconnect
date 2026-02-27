package com.larpconnect.njall.init;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Guice;
import com.google.inject.Module;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
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
  private final VertxProvider vertxProvider;
  private final AtomicReference<VerticleSetupService> setupServiceRef = new AtomicReference<>();

  VerticleLifecycle(List<Module> modules) {
    this(modules, new VertxProvider());
  }

  // Visible for testing
  VerticleLifecycle(List<Module> modules, VertxProvider vertxProvider) {
    this.modules = ImmutableList.copyOf(modules);
    this.vertxProvider = vertxProvider;
  }

  @Override
  protected void startUp() {
    logger.info("Starting VerticleLifecycle...");

    // Create temp Vertx for config loading
    var tempVertx = Vertx.vertx();
    JsonObject config;
    try {
      var url = Resources.getResource("config.json");
      var tempFile = java.nio.file.Files.createTempFile("config", ".json");
      tempFile.toFile().deleteOnExit();
      try (var is = url.openStream()) {
        java.nio.file.Files.copy(is, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      }
      String path = tempFile.toAbsolutePath().toString();

      ConfigStoreOptions fileStore =
          new ConfigStoreOptions().setType("file").setConfig(new JsonObject().put("path", path));
      ConfigStoreOptions sysStore = new ConfigStoreOptions().setType("sys");
      ConfigStoreOptions envStore = new ConfigStoreOptions().setType("env");

      ConfigRetrieverOptions options =
          new ConfigRetrieverOptions().addStore(fileStore).addStore(sysStore).addStore(envStore);

      ConfigRetriever retriever = ConfigRetriever.create(tempVertx, options);
      CompletableFuture<JsonObject> future = new CompletableFuture<>();
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
    } catch (java.io.IOException e) {
      throw new RuntimeException("Failed to load config file", e);
    } finally {
      tempVertx.close();
    }

    var vertx = vertxProvider.get();

    // Create a mutable list to add our internal modules
    var builder = ImmutableList.<Module>builder();
    builder.addAll(modules);
    builder.add(new VertxModule(vertxProvider));
    builder.add(new ConfigModule(config));

    // Create Guice Injector
    var injector = Guice.createInjector(builder.build());

    // Setup Verticle Factory via VerticleSetupService
    var setupService = injector.getInstance(VerticleSetupService.class);
    setupService.setup(vertx, injector);
    setupServiceRef.set(setupService);

    logger.info("VerticleLifecycle started.");
  }

  @Override
  protected void shutDown() {
    logger.info("Stopping VerticleLifecycle...");
    var vertx = vertxProvider.get();
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
  public void deploy(Class<? extends Verticle> verticleClass) {
    var service = setupServiceRef.get();
    if (service != null) {
      service.deploy(verticleClass);
    } else {
      throw new IllegalStateException("VerticleLifecycle not started");
    }
  }
}
