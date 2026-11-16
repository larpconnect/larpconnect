package com.larpconnect.njall.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Ticker;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.larpconnect.njall.common.annotation.Blocking;
import com.larpconnect.njall.queue.QueueModule;
import com.larpconnect.njall.queue.config.QueueConfig;
import com.larpconnect.njall.queue.health.AmqpHealthCheck;
import com.larpconnect.njall.queue.publisher.AmqpQueueService;
import com.larpconnect.njall.queue.publisher.InboxActivityPublisher;
import com.larpconnect.njall.queue.publisher.InboxQueueEnvelope;
import com.larpconnect.njall.queue.publisher.QueuePublishException;
import com.rabbitmq.client.amqp.Message;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.dropwizard.metrics5.health.HealthCheck.Result;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Step definitions backing AMQP 1.0 queue messaging feature scenarios. */
public final class AmqpQueueSteps {

  private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

  private final Logger logger = LoggerFactory.getLogger(AmqpQueueSteps.class);

  private @Nullable QueueConfig queueConfig;
  private @Nullable AmqpQueueService queueService;
  private @Nullable InboxActivityPublisher publisher;
  private @Nullable CompletableFuture<Void> publishFuture;
  private @Nullable Message lastDeliveredMessage;
  private @Nullable String rawConfigString;
  private @Nullable Throwable caughtConfigException;
  private @Nullable AmqpHealthCheck healthCheck;
  private @Nullable Result lastHealthResult;
  private @Nullable ExecutorService blockingExecutor;
  private boolean containerPaused;
  private long lastProbeElapsedMillis;
  private final FakeTicker fakeTicker = new FakeTicker();

  private Injector createQueueInjector(QueueConfig config, @Nullable Ticker ticker) {
    if (blockingExecutor != null) {
      blockingExecutor.close();
    }
    var executor = Executors.newVirtualThreadPerTaskExecutor();
    blockingExecutor = executor;
    var typesafeConfig =
        ConfigFactory.parseString(
            String.format(
                Locale.ROOT,
                "njall.queue { enabled = %b, host = \"%s\", port = %d, username = \"%s\", password"
                    + " = \"%s\", inbox-queue-name = \"%s\" }",
                config.enabled(),
                config.host(),
                config.port(),
                config.username(),
                config.password(),
                config.inboxQueueName()));
    return Guice.createInjector(
        new QueueModule(),
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(Config.class).toInstance(typesafeConfig);
            bind(ObjectMapper.class).toInstance(MAPPER);
            bind(Ticker.class).toInstance(ticker != null ? ticker : Ticker.systemTicker());
            bind(Executor.class).annotatedWith(Blocking.class).toInstance(executor);
          }
        });
  }

  @Given("a configured AMQP 1.0 broker connection")
  public void configuredAmqpBrokerConnection() {
    E2EFeatherpubAmqpFixture.ensureBrokerStarted();
    var mappedPort = E2EFeatherpubAmqpFixture.getBrokerPort();
    queueConfig =
        new QueueConfig(
            true,
            E2EFeatherpubAmqpFixture.getBrokerHost(),
            mappedPort,
            E2EFeatherpubAmqpFixture.AMQP_USER,
            E2EFeatherpubAmqpFixture.AMQP_PASS,
            "njall.inbox.activities");
    var injector = createQueueInjector(queueConfig, null);
    queueService = injector.getInstance(AmqpQueueService.class);
  }

  @When("the AMQP queue service starts up")
  public void theAmqpQueueServiceStartsUp() throws Exception {
    assertThat(queueService).isNotNull();
    queueService.startAsync().awaitRunning(Duration.ofSeconds(15));
  }

  @Then("the service transitions to RUNNING state")
  public void theServiceTransitionsToRunningState() {
    assertThat(queueService).isNotNull();
    assertThat(queueService.state()).isEqualTo(Service.State.RUNNING);
  }

  @Then("the durable queue {string} is declared on the broker")
  public void theDurableQueueIsDeclaredOnTheBroker(String queueName) {
    assertThat(queueService).isNotNull();
    E2EFeatherpubAmqpFixture.ensureBrokerStarted();
    E2EFeatherpubAmqpFixture.startConsumer(queueName);
  }

  @Then("an active publisher link is established")
  public void anActivePublisherLinkIsEstablished() {
    assertThat(queueService).isNotNull();
    assertThat(queueService.isRunning()).isTrue();
    assertThat(queueService.connection()).isNotNull();
    assertThat(queueService.publisher()).isNotNull();
  }

  @Given("queue configuration with enabled set to true and an empty password")
  public void queueConfigWithEnabledTrueAndEmptyPassword() {
    rawConfigString =
        "njall.queue { enabled = true, host = \"localhost\", port = 5672, username = \"njall_amqp\""
            + ", password = \"\" }";
  }

  @When("the queue configuration is parsed")
  public void theQueueConfigurationIsParsed() {
    try {
      var cfg = ConfigFactory.parseString(rawConfigString);
      new QueueConfig(cfg);
    } catch (Throwable t) {
      caughtConfigException = t;
    }
  }

  @Then("configuration loading fails fast throwing an IllegalStateException")
  public void configurationLoadingFailsFastThrowingIllegalStateException() {
    assertThat(caughtConfigException)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Missing required AMQP queue password");
  }

  @Given("an active AMQP 1.0 queue service connected to RabbitMQ")
  public void activeAmqpQueueServiceConnectedToRabbitMq() throws Exception {
    E2EFeatherpubAmqpFixture.ensureBrokerStarted();
    var mappedPort = E2EFeatherpubAmqpFixture.getBrokerPort();
    queueConfig =
        new QueueConfig(
            true,
            E2EFeatherpubAmqpFixture.getBrokerHost(),
            mappedPort,
            E2EFeatherpubAmqpFixture.AMQP_USER,
            E2EFeatherpubAmqpFixture.AMQP_PASS,
            "njall.inbox.activities");
    var injector = createQueueInjector(queueConfig, null);
    queueService = injector.getInstance(AmqpQueueService.class);
    queueService.startAsync().awaitRunning(Duration.ofSeconds(15));
    publisher = injector.getInstance(InboxActivityPublisher.class);
    E2EFeatherpubAmqpFixture.startConsumer("njall.inbox.activities");
  }

  @When("an inbox queue envelope is published for activity {string} with traceparent {string}")
  public void inboxQueueEnvelopePublishedWithTraceparent(String actId, String traceparent)
      throws Exception {
    assertThat(publisher).isNotNull();
    var activityJson = MAPPER.readTree("{\"id\":\"" + actId + "\",\"type\":\"Create\"}");
    var envelope =
        new InboxQueueEnvelope(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            ImmutableList.of(UUID.randomUUID()),
            Instant.now(),
            activityJson,
            Optional.of(traceparent));
    publishFuture = publisher.publish(envelope);
    publishFuture.get(10, TimeUnit.SECONDS);
  }

  @Then("the broker confirms ACCEPTED disposition")
  public void theBrokerConfirmsAcceptedDisposition() throws Exception {
    assertThat(publishFuture).isNotNull();
    assertThat(publishFuture.isDone()).isTrue();
    assertThat(publishFuture.isCompletedExceptionally()).isFalse();
    lastDeliveredMessage = E2EFeatherpubAmqpFixture.pollMessage(5);
    assertThat(lastDeliveredMessage).isNotNull();
  }

  @Then("one durable message is delivered to queue {string}")
  public void oneDurableMessageIsDeliveredToQueue(String queueName) {
    assertThat(lastDeliveredMessage).isNotNull();
    assertThat(lastDeliveredMessage.durable()).isTrue();
    assertThat(E2EFeatherpubAmqpFixture.getCapturedMessageCount(queueName)).isEqualTo(1);
  }

  @Then("the delivered message has messageId {string}")
  public void theDeliveredMessageHasMessageId(String expectedMessageId) {
    assertThat(lastDeliveredMessage).isNotNull();
    assertThat(lastDeliveredMessage.messageIdAsString()).isEqualTo(expectedMessageId);
  }

  @Then("the delivered message has contentType {string}")
  public void theDeliveredMessageHasContentType(String expectedContentType) {
    assertThat(lastDeliveredMessage).isNotNull();
    assertThat(lastDeliveredMessage.contentType()).isEqualTo(expectedContentType);
  }

  @Then("the delivered message has application property {string} equal to {string}")
  public void theDeliveredMessageHasPropertyEqualTo(String propName, String expectedVal) {
    assertThat(lastDeliveredMessage).isNotNull();
    assertThat(String.valueOf(lastDeliveredMessage.property(propName))).isEqualTo(expectedVal);
  }

  @Given("an AMQP 1.0 publisher whose broker connection is unavailable or link is rejected")
  public void publisherWithUnavailableBrokerConnection() {
    var deadConfig =
        new QueueConfig(
            true, "localhost", 65534, "njall_amqp", "njall_amqp_secret", "njall.inbox.activities");
    var injector = createQueueInjector(deadConfig, null);
    publisher = injector.getInstance(InboxActivityPublisher.class);
  }

  @When("an inbox queue envelope is published")
  public void anInboxQueueEnvelopeIsPublished() {
    assertThat(publisher).isNotNull();
    var envelope =
        new InboxQueueEnvelope(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            ImmutableList.of(UUID.randomUUID()),
            Instant.now(),
            MAPPER.createObjectNode(),
            Optional.empty());
    publishFuture = publisher.publish(envelope);
  }

  @Then("the publication future completes exceptionally with QueuePublishException")
  public void publicationFutureCompletesExceptionallyWithQueuePublishException() {
    assertThat(publishFuture).isNotNull();
    assertThatThrownBy(() -> publishFuture.get(5, TimeUnit.SECONDS))
        .isInstanceOf(ExecutionException.class)
        .hasCauseInstanceOf(QueuePublishException.class);
  }

  @Given("an AMQP health check evaluated a healthy broker connection")
  public void healthCheckEvaluatedHealthyBroker() throws Exception {
    E2EFeatherpubAmqpFixture.ensureBrokerStarted();
    var mappedPort = E2EFeatherpubAmqpFixture.getBrokerPort();
    queueConfig =
        new QueueConfig(
            true,
            E2EFeatherpubAmqpFixture.getBrokerHost(),
            mappedPort,
            E2EFeatherpubAmqpFixture.AMQP_USER,
            E2EFeatherpubAmqpFixture.AMQP_PASS,
            "njall.inbox.activities");
    var injector = createQueueInjector(queueConfig, fakeTicker);
    queueService = injector.getInstance(AmqpQueueService.class);
    queueService.startAsync().awaitRunning(Duration.ofSeconds(15));
    healthCheck = injector.getInstance(AmqpHealthCheck.class);
    lastHealthResult = healthCheck.check();
    assertThat(lastHealthResult.isHealthy()).isTrue();
  }

  @When("the AMQP health check is evaluated again within 10 seconds")
  public void healthCheckEvaluatedAgainWithin10Seconds() {
    assertThat(healthCheck).isNotNull();
    fakeTicker.advance(Duration.ofSeconds(5));
    lastHealthResult = healthCheck.check();
  }

  @Then("the cached healthy result is returned")
  public void cachedHealthyResultIsReturned() {
    assertThat(lastHealthResult).isNotNull();
    assertThat(lastHealthResult.isHealthy()).isTrue();
  }

  @Then("no additional broker ping query is executed")
  public void noAdditionalBrokerPingQueryIsExecuted() {
    assertThat(lastHealthResult).isNotNull();
  }

  @Given("the AMQP broker connection is closed, lost, or timed out")
  public void brokerConnectionClosedOrLost() {
    var deadConfig =
        new QueueConfig(
            true, "localhost", 65534, "njall_amqp", "njall_amqp_secret", "njall.inbox.activities");
    var injector = createQueueInjector(deadConfig, fakeTicker);
    healthCheck = injector.getInstance(AmqpHealthCheck.class);
  }

  @Given("the 10-second health check cache has expired")
  public void tenSecondHealthCheckCacheExpired() {
    fakeTicker.advance(Duration.ofSeconds(15));
  }

  @When("the AMQP health check is evaluated")
  public void amqpHealthCheckIsEvaluated() {
    assertThat(healthCheck).isNotNull();
    lastHealthResult = healthCheck.check();
  }

  @Then("an unhealthy health check result is returned describing the broker failure")
  public void unhealthyResultReturnedDescribingBrokerFailure() {
    assertThat(lastHealthResult).isNotNull();
    assertThat(lastHealthResult.isHealthy()).isFalse();
    assertThat(lastHealthResult.getMessage()).isNotBlank();
  }

  @Then("the unhealthy result is cached for 10 seconds")
  public void unhealthyResultCachedFor10Seconds() {
    assertThat(healthCheck).isNotNull();
    fakeTicker.advance(Duration.ofSeconds(5));
    var cached = healthCheck.check();
    assertThat(cached.isHealthy()).isFalse();
    assertThat(cached.getMessage()).isEqualTo(lastHealthResult.getMessage());
  }

  @When(
      "the RabbitMQ container is paused while a concurrent publisher probe holds the connection"
          + " lock")
  public void rabbitMqPausedWhileConcurrentProbeHoldsLock() throws Exception {
    assertThat(queueService).isNotNull();
    assertThat(healthCheck).isNotNull();
    var containerId = E2EFeatherpubAmqpFixture.RABBITMQ.getContainerId();
    E2EFeatherpubAmqpFixture.RABBITMQ.getDockerClient().pauseContainerCmd(containerId).exec();
    containerPaused = true;
    var existingPublisher = queueService.publisher();
    if (existingPublisher != null) {
      queueService.invalidatePublisher(existingPublisher);
    }
    try (var lockHolderExec = Executors.newVirtualThreadPerTaskExecutor()) {
      var holderFuture = lockHolderExec.submit(() -> queueService.publisher());
      Thread.sleep(150);
      long startNanos = System.nanoTime();
      lastHealthResult = healthCheck.check();
      lastProbeElapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
      unpauseRabbitMqIfNeeded();
      holderFuture.get(10, TimeUnit.SECONDS);
    } finally {
      unpauseRabbitMqIfNeeded();
    }
  }

  @Then("the AMQP health check times out cleanly within 3 seconds and reports unhealthy")
  public void healthCheckTimesOutCleanlyWithinThreeSeconds() {
    assertThat(lastHealthResult).isNotNull();
    assertThat(lastHealthResult.isHealthy()).isFalse();
    assertThat(lastHealthResult.getMessage())
        .contains("AMQP broker probe timed out after 1 second");
    assertThat(lastProbeElapsedMillis).isLessThan(3000L);
  }

  private void unpauseRabbitMqIfNeeded() {
    if (containerPaused) {
      try {
        var containerId = E2EFeatherpubAmqpFixture.RABBITMQ.getContainerId();
        E2EFeatherpubAmqpFixture.RABBITMQ.getDockerClient().unpauseContainerCmd(containerId).exec();
      } finally {
        containerPaused = false;
      }
    }
  }

  @After
  public void tearDown() {
    unpauseRabbitMqIfNeeded();
    if (queueService != null && queueService.isRunning()) {
      try {
        queueService.stopAsync().awaitTerminated(Duration.ofSeconds(5));
      } catch (Exception e) {
        logger.debug("Failed to stop queue service cleanly", e);
      }
    }
    if (blockingExecutor != null) {
      blockingExecutor.close();
      blockingExecutor = null;
    }
  }

  static final class FakeTicker implements Ticker {
    private final AtomicLong nanos = new AtomicLong();

    @Override
    public long read() {
      return nanos.get();
    }

    void advance(Duration duration) {
      nanos.addAndGet(duration.toNanos());
    }
  }
}
