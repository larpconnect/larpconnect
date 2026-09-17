package com.larpconnect.njall.server;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.larpconnect.njall.server.http.HttpServerService;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.CoordinatedShutdown;
import org.apache.pekko.actor.typed.ActorSystem;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DefaultServerManagerService extends AbstractIdleService
    implements ServerManagerService {

  private static final Duration SHUTDOWN_TIMEOUT = Duration.ofMinutes(2);
  private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(15);
  private static final Duration HTTP_STOP_TIMEOUT = Duration.ofSeconds(10);

  private final Logger logger = LoggerFactory.getLogger(DefaultServerManagerService.class);
  private final ShutdownHookRegistrar shutdownHookRegistrar;
  private final ActorSystem<Void> actorSystem;
  private final HttpServerService httpServerService;
  private @Nullable Thread shutdownHook;

  @Inject
  DefaultServerManagerService(
      ShutdownHookRegistrar shutdownHookRegistrar,
      ActorSystem<Void> actorSystem,
      HttpServerService httpServerService) {
    this.shutdownHookRegistrar = shutdownHookRegistrar;
    this.actorSystem = actorSystem;
    this.httpServerService = httpServerService;
  }

  @Override
  protected void startUp() throws Exception {
    initShutdownHook();
    startHttpServer();
  }

  private void initShutdownHook() {
    var hook = createShutdownThread();
    storeAndRegisterHook(hook);
  }

  private void storeAndRegisterHook(Thread hook) {
    this.shutdownHook = hook;
    shutdownHookRegistrar.registerShutdownHook(hook);
  }

  private void startHttpServer() throws Exception {
    try {
      httpServerService
          .start()
          .toCompletableFuture()
          .get(STARTUP_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
    } catch (Exception e) {
      logger.error("Failed to bind server socket", e);
      throw e;
    }
  }

  private void stopHttpServer() {
    try {
      httpServerService
          .stop()
          .toCompletableFuture()
          .get(HTTP_STOP_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
    } catch (Exception e) {
      logger.warn("Failed to cleanly stop HTTP server", e);
    }
  }

  @Override
  protected void shutDown() throws Exception {
    stopHttpServer();
    unregisterShutdownHook();
    runCoordinatedShutdown();
  }

  private void unregisterShutdownHook() {
    if (shutdownHook != null) {
      try {
        shutdownHookRegistrar.removeShutdownHook(shutdownHook);
      } catch (Exception e) {
        logger.debug("Failed to remove shutdown hook", e);
      }
    }
  }

  private void runCoordinatedShutdown() throws Exception {
    CoordinatedShutdown.get(actorSystem)
        .runAll(CoordinatedShutdown.jvmExitReason())
        .toCompletableFuture()
        .get(SHUTDOWN_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
  }

  Thread createShutdownThread() {
    return new Thread(this::onJvmShutdown, "pekko-coordinated-shutdown");
  }

  private void onJvmShutdown() {
    logger.info("JVM shutdown initiated; invoking CoordinatedShutdown...");
    try {
      stopAsync().awaitTerminated(SHUTDOWN_TIMEOUT);
    } catch (Exception e) {
      logger.warn("CoordinatedShutdown failed on JVM exit", e);
    }
  }
}
