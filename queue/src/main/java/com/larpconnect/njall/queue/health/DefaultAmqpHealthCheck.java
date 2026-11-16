package com.larpconnect.njall.queue.health;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.google.inject.Inject;
import com.larpconnect.njall.common.annotation.Blocking;
import com.larpconnect.njall.queue.config.QueueConfig;
import com.larpconnect.njall.queue.publisher.AmqpClient;
import com.rabbitmq.client.amqp.Publisher;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;

/**
 * Default implementation of {@link AmqpHealthCheck} backed by a 10-second Caffeine {@link
 * AsyncCache}.
 */
final class DefaultAmqpHealthCheck implements AmqpHealthCheck {

  static final Duration DEFAULT_CACHE_TTL = Duration.ofSeconds(10);
  static final Duration PROBE_TIMEOUT = Duration.ofSeconds(1);
  private static final String CACHE_KEY = "amqp_broker_ping";

  private final AmqpClient amqpClient;
  private final QueueConfig config;
  private final Duration probeTimeout;
  private final AsyncCache<String, Result> cache;

  @Inject
  DefaultAmqpHealthCheck(
      AmqpClient amqpClient, QueueConfig config, Ticker ticker, @Blocking Executor probeExecutor) {
    this(amqpClient, config, ticker, probeExecutor, DEFAULT_CACHE_TTL);
  }

  DefaultAmqpHealthCheck(
      AmqpClient amqpClient,
      QueueConfig config,
      Ticker ticker,
      Executor probeExecutor,
      Duration cacheTtl) {
    this(amqpClient, config, ticker, probeExecutor, cacheTtl, PROBE_TIMEOUT);
  }

  DefaultAmqpHealthCheck(
      AmqpClient amqpClient,
      QueueConfig config,
      Ticker ticker,
      Executor probeExecutor,
      Duration cacheTtl,
      Duration probeTimeout) {
    this.amqpClient = amqpClient;
    this.config = config;
    this.probeTimeout = probeTimeout;
    this.cache = buildCache(cacheTtl, ticker, probeExecutor);
  }

  private static AsyncCache<String, Result> buildCache(
      Duration cacheTtl, Ticker ticker, Executor probeExecutor) {
    AsyncCache<String, Result> built =
        Caffeine.newBuilder()
            .expireAfterWrite(cacheTtl)
            .ticker(ticker)
            .executor(probeExecutor)
            .buildAsync();
    built.synchronous().invalidateAll();
    return built;
  }

  @Override
  public Result check() {
    if (Thread.currentThread().isInterrupted()) {
      return Result.unhealthy("AMQP broker probe interrupted");
    }
    return awaitCachedProbe();
  }

  @Override
  public void invalidateCache() {
    cache.synchronous().invalidateAll();
  }

  private Result awaitCachedProbe() {
    CompletableFuture<Result> future;
    try {
      future = cache.get(CACHE_KEY, this::startProbe);
    } catch (RuntimeException _) {
      return Result.unhealthy("AMQP broker probe rejected by executor");
    }
    return awaitFutureResult(future);
  }

  private Result awaitFutureResult(CompletableFuture<Result> future) {
    try {
      return future.get();
    } catch (InterruptedException ex) {
      future.cancel(true);
      awaitFailedProbeEviction();
      Thread.currentThread().interrupt();
      return Result.unhealthy("AMQP broker probe interrupted: " + ex.getMessage());
    } catch (CancellationException | ExecutionException ex) {
      awaitFailedProbeEviction();
      return Result.unhealthy("AMQP broker probe interrupted: " + extractFailureMessage(ex));
    }
  }

  private void awaitFailedProbeEviction() {
    while (Optional.ofNullable(cache.getIfPresent(CACHE_KEY))
        .filter(CompletableFuture::isCompletedExceptionally)
        .isPresent()) {
      Thread.onSpinWait();
    }
  }

  static String extractFailureMessage(Throwable ex) {
    var cause = ex.getCause();
    return cause != null ? String.valueOf(cause.getMessage()) : String.valueOf(ex.getMessage());
  }

  private CompletableFuture<Result> startProbe(String key, Executor executor) {
    if (!config.enabled()) {
      return CompletableFuture.completedFuture(Result.healthy("AMQP queue disabled"));
    }
    if (!amqpClient.isRunning()) {
      return CompletableFuture.completedFuture(
          Result.unhealthy("AMQP queue service is not running"));
    }
    return dispatchBrokerProbe(executor);
  }

  private CompletableFuture<Result> dispatchBrokerProbe(Executor executor) {
    var probedPublisher = createPublisherRef();
    var callerCancelled = createCancelledFlag();
    var settled = createSettledFlag();
    var probeTask = createProbeTask(probedPublisher, callerCancelled);
    var probeFuture = createProbeFuture(probeTask, probedPublisher, callerCancelled, settled);
    executor.execute(() -> executeProbeTask(probeTask, probeFuture));
    scheduleProbeTimeout(probeFuture);
    return probeFuture;
  }

  private FutureTask<Result> createProbeTask(
      AtomicReference<@Nullable Publisher> probedPublisher, AtomicBoolean callerCancelled) {
    return new FutureTask<>(() -> evaluateConnection(probedPublisher, callerCancelled));
  }

  private static void executeProbeTask(FutureTask<Result> probeTask, ProbeFuture probeFuture) {
    probeTask.run();
    probeFuture.completeFromTask();
  }

  private void scheduleProbeTimeout(ProbeFuture probeFuture) {
    CompletableFuture.delayedExecutor(probeTimeout.toMillis(), TimeUnit.MILLISECONDS)
        .execute(() -> probeFuture.completeOnTimeout(amqpClient));
  }

  private static AtomicReference<@Nullable Publisher> createPublisherRef() {
    return new AtomicReference<>();
  }

  private static AtomicBoolean createCancelledFlag() {
    return new AtomicBoolean(false);
  }

  private static AtomicBoolean createSettledFlag() {
    return new AtomicBoolean(false);
  }

  private static ProbeFuture createProbeFuture(
      FutureTask<Result> probeTask,
      AtomicReference<@Nullable Publisher> probedPublisher,
      AtomicBoolean callerCancelled,
      AtomicBoolean settled) {
    return new ProbeFuture(probeTask, probedPublisher, callerCancelled, settled);
  }

  private Result evaluateConnection(
      AtomicReference<@Nullable Publisher> probedPublisher, AtomicBoolean callerCancelled)
      throws InterruptedException {
    return amqpClient
        .probeConnection(probedPublisher::set, callerCancelled::get)
        .map(
            info ->
                Result.healthy(
                    "AMQP connected to "
                        + info.host()
                        + ":"
                        + info.port()
                        + " ("
                        + info.brokerProductName()
                        + ")"))
        .orElseGet(() -> Result.unhealthy("AMQP broker connection is not active"));
  }

  private static final class ProbeFuture extends CompletableFuture<Result> {
    private final FutureTask<Result> probeTask;
    private final AtomicReference<@Nullable Publisher> probedPublisher;
    private final AtomicBoolean callerCancelled;
    private final AtomicBoolean settled;

    private ProbeFuture(
        FutureTask<Result> probeTask,
        AtomicReference<@Nullable Publisher> probedPublisher,
        AtomicBoolean callerCancelled,
        AtomicBoolean settled) {
      this.probeTask = probeTask;
      this.probedPublisher = probedPublisher;
      this.callerCancelled = callerCancelled;
      this.settled = settled;
    }

    void completeFromTask() {
      if (!settled.compareAndSet(false, true)) {
        return;
      }
      if (probeTask.state() == Future.State.SUCCESS) {
        complete(probeTask.resultNow());
        return;
      }
      var cause = probeTask.exceptionNow();
      if (cause instanceof InterruptedException) {
        completeExceptionally(createCancellation(cause.getMessage()));
        return;
      }
      complete(Result.unhealthy("AMQP broker probe failed: " + cause.getMessage()));
    }

    private static CancellationException createCancellation(@Nullable String message) {
      return new CancellationException(message);
    }

    void completeOnTimeout(AmqpClient amqpClient) {
      if (!settled.compareAndSet(false, true)) {
        return;
      }
      var stuckPublisher = probedPublisher.get();
      if (stuckPublisher != null) {
        amqpClient.invalidatePublisher(stuckPublisher);
      }
      probeTask.cancel(true);
      complete(Result.unhealthy("AMQP broker probe timed out after 1 second"));
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      if (!settled.compareAndSet(false, true)) {
        return false;
      }
      callerCancelled.set(true);
      boolean cancelled = super.cancel(mayInterruptIfRunning);
      probeTask.cancel(mayInterruptIfRunning);
      return cancelled;
    }
  }
}
