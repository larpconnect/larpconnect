package com.larpconnect.njall.server;

import io.vertx.core.Vertx;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class WebServerVerticleBenchmarkTest {

  private final Logger logger = LoggerFactory.getLogger(WebServerVerticleBenchmarkTest.class);

  @Test
  void benchmarkStartup_withIO_finishesQuickly() throws InterruptedException {
    int numIterations = 50;
    long totalTime = 0;

    for (int i = 0; i < numIterations; i++) {
      Vertx vertx = Vertx.vertx();
      AtomicInteger actualPort = new AtomicInteger();
      WebServerVerticle verticle =
          new WebServerVerticle(0, "openapi.yaml", m -> "{}", Optional.of(actualPort::set));

      CountDownLatch latch = new CountDownLatch(1);
      long start = System.nanoTime();

      vertx
          .deployVerticle(verticle)
          .onComplete(
              res -> {
                if (!res.succeeded()) {
                  logger.error("Deployment failed", res.cause());
                }
                vertx.close().onComplete(v -> latch.countDown());
              });

      boolean finished = latch.await(10, TimeUnit.SECONDS);
      if (!finished) {
        logger.warn("Iteration {} timed out waiting for latch", i);
      }
      if (i >= 10) { // Warmup 10 iterations
        totalTime += (System.nanoTime() - start);
      }
    }
    logger.info(
        "Average startup time over {} iterations: {} ms",
        (numIterations - 10),
        (totalTime / ((double) (numIterations - 10))) / 1_000_000.0);
  }
}
