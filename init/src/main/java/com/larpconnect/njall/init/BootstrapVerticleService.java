package com.larpconnect.njall.init;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.ConfigurationException;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.larpconnect.njall.common.time.TimeService;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BootstrapVerticleService extends AbstractIdleService implements VerticleService {
  private static final int CONFIG_LOAD_TIMEOUT_SECONDS = 5;
  private final Logger logger = LoggerFactory.getLogger(BootstrapVerticleService.class);

  private final ImmutableList<Module> modules;
  private final AtomicReference<VerticleService> delegateRef = new AtomicReference<>();

  BootstrapVerticleService(ImmutableList<Module> modules) {
    this.modules = modules;
  }

  @Override
  protected void startUp() {
    logger.info("Starting BootstrapVerticleService...");

    // Load default config
    JsonObject defaultConfig;
    try {
      var configResource = System.getProperty("njall.config.resource", "config.json");
      var url = Resources.getResource(configResource);
      defaultConfig = new JsonObject(Resources.toString(url, StandardCharsets.UTF_8));
    } catch (IllegalArgumentException | IOException e) {
      throw new IllegalStateException("Failed to load default config", e);
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

      config = future.get(CONFIG_LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while loading config", e);
    } catch (ExecutionException | TimeoutException e) {
      throw new IllegalStateException("Failed to load config", e);
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

    // Initialize Vertx and setup service via the lifecycle instance injected
    var delegate = injector.getInstance(VerticleService.class);
    delegateRef.set(delegate);
    delegate.startAsync().awaitRunning();

    try {
      var timeService = injector.getInstance(TimeService.class);
      timeService.startAsync().awaitRunning();
    } catch (ConfigurationException e) {
      logger.debug("TimeService not bound, skipping start.");
    }

    logger.info("BootstrapVerticleService started.");
  }

  @Override
  protected void shutDown() {
    logger.info("Stopping BootstrapVerticleService...");
    delegateRef.get().stopAsync().awaitTerminated();
  }

  @Override
  public void deploy(Class<? extends Verticle> verticleClass) {
    if (!isRunning()) {
      throw new IllegalStateException("BootstrapVerticleService not started");
    }
    delegateRef.get().deploy(verticleClass);
  }
}
