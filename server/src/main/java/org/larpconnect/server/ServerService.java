package org.larpconnect.server;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import io.vertx.core.Vertx;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.larpconnect.data.DatabaseInitializer;
import org.larpconnect.events.MainVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Lifecycle service for LarpConnect server using Guava's AbstractIdleService. */
final class ServerService extends AbstractIdleService {
  private static final Logger logger = LoggerFactory.getLogger(ServerService.class);

  private final Provider<Vertx> vertxProvider;
  private final Provider<MainVerticle> mainVerticleProvider;
  private final DatabaseInitializer databaseInitializer;
  private volatile String deploymentId;

  @Inject
  ServerService(
      Provider<Vertx> vertxProvider,
      Provider<MainVerticle> mainVerticleProvider,
      DatabaseInitializer databaseInitializer) {
    this.vertxProvider = vertxProvider;
    this.mainVerticleProvider = mainVerticleProvider;
    this.databaseInitializer = databaseInitializer;
  }

  @Override
  protected void startUp() throws Exception {
    logger.info("Starting ServerService...");

    logger.info("Running database migrations...");
    databaseInitializer.migrate();

    Vertx vertx = vertxProvider.get();
    MainVerticle mainVerticle = mainVerticleProvider.get();

    CompletableFuture<String> future = new CompletableFuture<>();
    vertx
        .deployVerticle(mainVerticle)
        .onSuccess(
            id -> {
              logger.info("MainVerticle deployed successfully with ID: {}", id);
              future.complete(id);
            })
        .onFailure(
            err -> {
              logger.error("Failed to deploy MainVerticle", err);
              future.completeExceptionally(err);
            });

    try {
      deploymentId = future.get(30, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      logger.error("Timeout or failure during MainVerticle deployment", e);
      throw new RuntimeException("Failed to start server", e);
    }
  }

  @Override
  protected void shutDown() throws Exception {
    logger.info("Stopping ServerService...");
    Vertx vertx = vertxProvider.get();
    CompletableFuture<Void> future = new CompletableFuture<>();
    vertx
        .close()
        .onSuccess(
            v -> {
              logger.info("Vert.x instance closed successfully.");
              future.complete(null);
            })
        .onFailure(
            err -> {
              logger.error("Failed to close Vert.x instance", err);
              future.completeExceptionally(err);
            });

    try {
      future.get(30, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      logger.error("Timeout or failure during Vert.x shutdown", e);
      throw new RuntimeException("Failed to stop server", e);
    }
  }

  /**
   * Returns the deployment ID of the MainVerticle.
   *
   * @return The deployment ID or null if not deployed.
   */
  public String getDeploymentId() {
    return deploymentId;
  }
}
