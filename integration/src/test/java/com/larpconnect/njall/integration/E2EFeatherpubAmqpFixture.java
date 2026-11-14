package com.larpconnect.njall.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.larpconnect.njall.api.studios.inbox.InboxDeduplicationCache;
import com.larpconnect.njall.data.cache.StudioLookupCacheService;
import com.larpconnect.njall.queue.health.AmqpHealthCheck;
import com.larpconnect.njall.queue.publisher.AmqpQueueService;
import com.larpconnect.njall.server.ServerModule;
import com.larpconnect.njall.server.http.HttpServerService;
import com.rabbitmq.client.amqp.AmqpException;
import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Consumer;
import com.rabbitmq.client.amqp.Environment;
import com.rabbitmq.client.amqp.Message;
import com.rabbitmq.client.amqp.impl.AmqpEnvironmentBuilder;
import com.typesafe.config.ConfigFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.pekko.actor.typed.ActorSystem;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/** Test fixture managing RabbitMQ container, AMQP client, HTTP server, and DB helpers. */
final class E2EFeatherpubAmqpFixture {

  public static final String QUEUE_NAME = "njall.inbox.activities";
  public static final String AMQP_USER = "njall_amqp";
  public static final String AMQP_PASS = "njall_amqp_secret";
  private static final int AMQP_PORT = 5672;
  private static final Duration TIMEOUT = Duration.ofSeconds(30);
  private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
  private static final ReentrantLock BROKER_LOCK = new ReentrantLock();

  // Lifecycle managed across the integration suite via @BeforeAll and @AfterAll hooks.
  @SuppressWarnings("resource")
  static final GenericContainer<?> RABBITMQ =
      new GenericContainer<>(DockerImageName.parse("rabbitmq:4.3-alpine"))
          .withExposedPorts(AMQP_PORT)
          .withEnv("RABBITMQ_DEFAULT_USER", AMQP_USER)
          .withEnv("RABBITMQ_DEFAULT_PASS", AMQP_PASS)
          .waitingFor(Wait.forLogMessage(".*Server startup complete;.*\\n", 1));

  private static @Nullable Environment amqpEnvironment;
  private static @Nullable Connection amqpConnection;
  private static @Nullable Consumer activeConsumer;

  private static final AtomicReference<@Nullable String> LAST_SETTLEMENT_DISPOSITION =
      new AtomicReference<>();
  private static final AtomicReference<ResponseSnapshot> LAST_STATIC_RESPONSE =
      new AtomicReference<>(new ResponseSnapshot(Optional.empty(), Optional.empty()));
  private static final AtomicReference<@Nullable Injector> STATIC_INJECTOR =
      new AtomicReference<>();
  private static final AtomicReference<@Nullable StudioLookupCacheService> CACHE_SERVICE =
      new AtomicReference<>();

  private static final BlockingQueue<Message> RECEIVED_MESSAGES = new LinkedBlockingQueue<>();
  private static final CopyOnWriteArrayList<Message> RECORDED_MESSAGES =
      new CopyOnWriteArrayList<>();

  private final Logger logger = LoggerFactory.getLogger(E2EFeatherpubAmqpFixture.class);
  private final HttpClient httpClient =
      HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
  private @Nullable Injector injector;
  private @Nullable HttpServerService serverService;
  private @Nullable AmqpQueueService amqpQueueService;
  private @Nullable ActorSystem<Void> system;
  private volatile int boundPort;
  private @Nullable HttpResponse<String> lastResponse;
  private @Nullable JsonNode rootJson;

  E2EFeatherpubAmqpFixture() {}

  public static String getBrokerHost() {
    return RABBITMQ.getHost();
  }

  public static int getBrokerPort() {
    return RABBITMQ.getMappedPort(AMQP_PORT);
  }

  public static @Nullable HttpResponse<String> getLastStaticResponse() {
    return LAST_STATIC_RESPONSE.get().response().orElse(null);
  }

  public static @Nullable JsonNode getLastStaticJson() {
    return LAST_STATIC_RESPONSE.get().json().orElse(null);
  }

  private static void withBrokerLock(Runnable action) {
    BROKER_LOCK.lock();
    try {
      action.run();
    } finally {
      BROKER_LOCK.unlock();
    }
  }

  public static void ensureBrokerStarted() {
    withBrokerLock(E2EFeatherpubAmqpFixture::ensureBrokerStartedLocked);
  }

  private static void ensureBrokerStartedLocked() {
    if (!RABBITMQ.isRunning()) {
      RABBITMQ.start();
    }
    if (amqpConnection == null) {
      createConnectionAndEnvironmentLocked();
    }
  }

  private static void createConnectionAndEnvironmentLocked() {
    closeConnectionAndEnvironmentLocked();
    amqpEnvironment =
        new AmqpEnvironmentBuilder()
            .connectionSettings()
            .host(RABBITMQ.getHost())
            .port(RABBITMQ.getMappedPort(AMQP_PORT))
            .username(AMQP_USER)
            .password(AMQP_PASS)
            .connectionTimeout(Duration.ofSeconds(10))
            .environmentBuilder()
            .build();
    amqpConnection = amqpEnvironment.connectionBuilder().build();
    amqpConnection.management().queue(QUEUE_NAME).declare();
  }

  private static void closeConnectionAndEnvironmentLocked() {
    closeQuietly(amqpConnection);
    closeQuietly(amqpEnvironment);
    amqpConnection = null;
    amqpEnvironment = null;
  }

  private static void closeQuietly(@Nullable AutoCloseable resource) {
    if (resource != null) {
      try {
        resource.close();
      } catch (Exception ignored) {
        // Expected best-effort teardown cleanup when broker container stops or restarts.
      }
    }
  }

  public static void tearDownBroker() {
    withBrokerLock(
        () -> {
          closeActiveConsumerLocked();
          closeConnectionAndEnvironmentLocked();
          if (RABBITMQ.isRunning()) {
            RABBITMQ.stop();
          }
        });
  }

  public static void stopBrokerApp() throws Exception {
    BROKER_LOCK.lock();
    try {
      closeActiveConsumerLocked();
      closeConnectionAndEnvironmentLocked();
      RABBITMQ.execInContainer("rabbitmqctl", "stop_app");
    } finally {
      BROKER_LOCK.unlock();
    }
    clearHealthCheckCache();
  }

  public static void startBrokerApp() throws Exception {
    BROKER_LOCK.lock();
    try {
      RABBITMQ.execInContainer("rabbitmqctl", "start_app");
      createConnectionAndEnvironmentLocked();
      resumeConsumerLocked(QUEUE_NAME);
    } finally {
      BROKER_LOCK.unlock();
    }
    clearHealthCheckCache();
  }

  private static void resumeConsumerLocked(String queueName) {
    closeActiveConsumerLocked();
    ensureBrokerStartedLocked();
    var conn = Objects.requireNonNull(amqpConnection);
    try {
      conn.management().queue(queueName).declare();
    } catch (AmqpException e) {
      createConnectionAndEnvironmentLocked();
      conn = Objects.requireNonNull(amqpConnection);
      conn.management().queue(queueName).declare();
    }
    activeConsumer =
        conn.consumerBuilder()
            .queue(queueName)
            .messageHandler(
                (ctx, msg) -> {
                  RECEIVED_MESSAGES.add(msg);
                  RECORDED_MESSAGES.add(msg);
                  ctx.accept();
                  LAST_SETTLEMENT_DISPOSITION.set("ACCEPTED");
                })
            .build();
  }

  public static void startConsumer(String queueName) {
    clearCapturedMessages();
    withBrokerLock(() -> resumeConsumerLocked(queueName));
  }

  private static void closeActiveConsumerLocked() {
    closeQuietly(activeConsumer);
    activeConsumer = null;
  }

  public static void clearHealthCheckCache() {
    Optional.ofNullable(STATIC_INJECTOR.get())
        .ifPresent(inj -> inj.getInstance(AmqpHealthCheck.class).invalidateCache());
  }

  public void closeConsumer() {
    withBrokerLock(E2EFeatherpubAmqpFixture::closeActiveConsumerLocked);
  }

  public static void clearCapturedMessages() {
    RECEIVED_MESSAGES.clear();
    RECORDED_MESSAGES.clear();
    LAST_SETTLEMENT_DISPOSITION.set(null);
  }

  public @Nullable String getLastSettlementDisposition() {
    return LAST_SETTLEMENT_DISPOSITION.get();
  }

  public void clearDeduplicationCache() {
    Objects.requireNonNull(injector, "Injector is not initialized")
        .getInstance(InboxDeduplicationCache.class)
        .invalidateAll();
  }

  public static @Nullable Message pollMessage(long s) throws InterruptedException {
    return RECEIVED_MESSAGES.poll(s, TimeUnit.SECONDS);
  }

  public static int getCapturedMessageCount() {
    return RECORDED_MESSAGES.size();
  }

  public static @Nullable Message getCapturedMessage(int i) {
    var snapshot = ImmutableList.copyOf(RECORDED_MESSAGES);
    return (i >= 0 && i < snapshot.size()) ? snapshot.get(i) : null;
  }

  public static String extractBodyString(Message m) {
    var raw = m.body();
    return raw != null && raw.length > 0 ? new String(raw, StandardCharsets.UTF_8) : "";
  }

  public static String extractTraceparent(Message m) {
    if (m.hasProperty("traceparent")) {
      return String.valueOf(m.property("traceparent"));
    }
    return m.hasAnnotation("traceparent") ? String.valueOf(m.annotation("traceparent")) : "";
  }

  private static Message requireCapturedMessage(int idx) {
    var m = getCapturedMessage(idx);
    assertThat(m).isNotNull();
    return m;
  }

  public static void assertMessageId(int idx, String expectedId) {
    assertThat(requireCapturedMessage(idx).messageIdAsString()).isEqualTo(expectedId);
  }

  public static void assertDurable(int idx) {
    assertThat(requireCapturedMessage(idx).durable()).isTrue();
  }

  public static void assertTraceparent(int idx, String exp) {
    assertThat(extractTraceparent(requireCapturedMessage(idx))).isEqualTo(exp);
  }

  private static JsonNode getCapturedEnvelopeJson(int idx) throws Exception {
    return MAPPER.readTree(extractBodyString(requireCapturedMessage(idx)));
  }

  public static void assertTargetActor(int idx, String actorId) throws Exception {
    var targets = getCapturedEnvelopeJson(idx).path("targetActorIds").toString();
    assertThat(targets.toLowerCase(Locale.ROOT)).contains(actorId.toLowerCase(Locale.ROOT));
  }

  public static void assertReceivingActor(int idx, String actorId) throws Exception {
    assertThat(getCapturedEnvelopeJson(idx).path("receivingActorId").asText())
        .isEqualToIgnoringCase(actorId);
  }

  public static void assertPayloadField(int idx, String f, String exp) throws Exception {
    assertThat(getCapturedEnvelopeJson(idx).path("activity").path(f).asText()).isEqualTo(exp);
  }

  public static String computeContentDigestHeader(String alg, String p)
      throws NoSuchAlgorithmException {
    return E2EFeatherpubDataHelper.computeContentDigestHeader(alg, p);
  }

  public void startServer(String jdbcUrl, String adminPass, String usersPass) throws Exception {
    var h = RABBITMQ.getHost();
    var p = RABBITMQ.getMappedPort(AMQP_PORT);
    var cfg =
        String.format(
            Locale.ROOT,
            "larpconnect.server.port=0%nlarpconnect.data.database.admin={jdbc-url:\"%s\","
                + "username:\"njall_admin\",password:\"%s\"}%nlarpconnect.data.database.users={"
                + "jdbc-url:\"%s\",username:\"njall_users\",password:\"%s\"}%nnjall.queue={"
                + "enabled:true,host:\"%s\",port:%d,username:\"njall_amqp\",password:"
                + "\"njall_amqp_secret\"}%n",
            jdbcUrl,
            adminPass,
            jdbcUrl,
            usersPass,
            h,
            p);
    var config = ConfigFactory.parseString(cfg).withFallback(ConfigFactory.load());
    injector = Guice.createInjector(new ServerModule(config));
    STATIC_INJECTOR.set(injector);
    var cs = injector.getInstance(StudioLookupCacheService.class);
    CACHE_SERVICE.set(cs);
    cs.startAsync().awaitRunning(TIMEOUT);
    amqpQueueService = injector.getInstance(AmqpQueueService.class);
    amqpQueueService.startAsync().awaitRunning(TIMEOUT);
    serverService = injector.getInstance(HttpServerService.class);
    system = injector.getInstance(Key.get(new TypeLiteral<ActorSystem<Void>>() {}));
    serverService.start().toCompletableFuture().get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
    boundPort = serverService.getBoundPort();
  }

  public void stopServer() {
    try {
      if (serverService != null) {
        serverService.stop().toCompletableFuture().get(5, TimeUnit.SECONDS);
      }
      var cs = CACHE_SERVICE.getAndSet(null);
      if (cs != null) {
        cs.stopAsync().awaitTerminated(5, TimeUnit.SECONDS);
      }
      if (amqpQueueService != null) {
        amqpQueueService.stopAsync().awaitTerminated(5, TimeUnit.SECONDS);
      }
      if (system != null) {
        system.terminate();
        system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
      }
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      logger.warn("Server shutdown interrupted or failed", e);
    } finally {
      STATIC_INJECTOR.set(null);
    }
  }

  public void refreshCache() {
    Optional.ofNullable(CACHE_SERVICE.get()).ifPresent(StudioLookupCacheService::refresh);
  }

  public int getBoundPort() {
    return boundPort;
  }

  public @Nullable HttpResponse<String> getLastResponse() {
    return lastResponse;
  }

  public @Nullable JsonNode getRootJson() {
    return rootJson;
  }

  public void sendPost(String path, String body, Map<String, String> headers) throws Exception {
    var b = HttpRequest.newBuilder().uri(URI.create("http://127.0.0.1:" + boundPort + path));
    headers.forEach(b::header);
    var req = b.timeout(TIMEOUT).POST(HttpRequest.BodyPublishers.ofString(body)).build();
    lastResponse = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    parseResponse();
  }

  public void sendGet(String path) throws Exception {
    var u = URI.create("http://127.0.0.1:" + boundPort + path);
    var req = HttpRequest.newBuilder().uri(u).timeout(TIMEOUT).GET().build();
    lastResponse = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    parseResponse();
  }

  private void parseResponse() {
    var r = lastResponse;
    try {
      var b = r != null ? r.body() : null;
      rootJson = !Strings.isNullOrEmpty(b) ? MAPPER.readTree(b) : null;
    } catch (JsonProcessingException e) {
      logger.debug("Failed to parse response JSON", e);
      rootJson = null;
    }
    LAST_STATIC_RESPONSE.set(
        new ResponseSnapshot(Optional.ofNullable(r), Optional.ofNullable(rootJson)));
  }

  public static void executeSql(String url, String pwd, String sql, Object... params)
      throws SQLException {
    E2EFeatherpubDataHelper.executeSql(url, pwd, sql, params);
  }

  public static void truncateTables(String jdbcUrl, String password) throws SQLException {
    E2EFeatherpubDataHelper.truncateTables(jdbcUrl, password, CACHE_SERVICE.get());
  }

  public static boolean deduplicationRecordExists(
      String url, String pwd, String actId, String targetId) throws SQLException {
    return E2EFeatherpubDataHelper.deduplicationRecordExists(url, pwd, actId, targetId);
  }

  public static void expireDeduplicationRecord(String url, String pwd, String actId)
      throws SQLException {
    E2EFeatherpubDataHelper.expireDeduplicationRecord(url, pwd, actId);
  }

  public static boolean isDeduplicationVisibleToTenant(
      String url, String pwd, String actId, UUID tenantId) throws SQLException {
    return E2EFeatherpubDataHelper.isDeduplicationVisibleToTenant(url, pwd, actId, tenantId);
  }

  public static UUID[] provisionStudio(String url, String pwd, String alias, String name)
      throws SQLException {
    return E2EFeatherpubDataHelper.provisionStudio(url, pwd, alias, name, CACHE_SERVICE.get());
  }

  public static void provisionActor(
      String url, String pwd, UUID tId, UUID aId, String u, @Nullable UUID gen, String studio)
      throws SQLException {
    E2EFeatherpubDataHelper.provisionActor(url, pwd, tId, aId, u, gen, studio);
  }

  public static void provisionLineage(
      String url, String pwd, UUID tId, int d, String root, String leaf, String studio)
      throws SQLException {
    E2EFeatherpubDataHelper.provisionLineage(url, pwd, tId, d, root, leaf, studio);
  }

  public static void provisionCyclicRelationship(
      String url, String pwd, UUID tId, UUID a1, String u1, UUID a2, String u2, String studio)
      throws SQLException {
    E2EFeatherpubDataHelper.provisionCyclicRelationship(url, pwd, tId, a1, u1, a2, u2, studio);
  }

  public static void softDeleteActor(String url, String pwd, UUID id) throws SQLException {
    E2EFeatherpubDataHelper.softDeleteActor(url, pwd, id);
  }

  public static void softDeleteStudio(String url, String pwd, String alias) throws SQLException {
    E2EFeatherpubDataHelper.softDeleteStudio(url, pwd, alias, CACHE_SERVICE.get());
  }

  public static String substituteTimestamps(String in) {
    return E2EFeatherpubDataHelper.substituteTimestamps(in);
  }

  private record ResponseSnapshot(
      Optional<HttpResponse<String>> response, Optional<JsonNode> json) {}
}
