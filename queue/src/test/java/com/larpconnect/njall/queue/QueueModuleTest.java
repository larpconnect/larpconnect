package com.larpconnect.njall.queue;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Ticker;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Modules;
import com.larpconnect.njall.common.annotation.Blocking;
import com.larpconnect.njall.queue.config.QueueConfig;
import com.larpconnect.njall.queue.health.AmqpHealthCheck;
import com.larpconnect.njall.queue.publisher.AmqpClient;
import com.larpconnect.njall.queue.publisher.AmqpQueueService;
import com.larpconnect.njall.queue.publisher.FakeAmqpQueueService;
import com.larpconnect.njall.queue.publisher.InboxActivityPublisher;
import com.larpconnect.njall.queue.publisher.InboxQueueEnvelope;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.dropwizard.metrics5.health.HealthCheck;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class QueueModuleTest {

  private static Module createBaseTestModule() {
    var config =
        ConfigFactory.parseString(
            "njall.queue { enabled = false, host = \"amqp.test.local\", port = 5673 }");
    var mapper =
        new ObjectMapper().registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());
    return new AbstractModule() {
      @Override
      protected void configure() {
        bind(Config.class).toInstance(config);
        bind(ObjectMapper.class).toInstance(mapper);
        bind(Ticker.class).toInstance(Ticker.systemTicker());
        bind(Executor.class).annotatedWith(Blocking.class).toInstance(Runnable::run);
      }
    };
  }

  @Test
  @DisplayName("QueueModule wires queue config, service, publisher, and healthcheck multibinding")
  void queueModule_wiresBindingsAndEvaluatesDisabledHealthCheck() {
    var injector = Guice.createInjector(new QueueModule(), createBaseTestModule());

    var queueConfig = injector.getInstance(QueueConfig.class);
    assertThat(queueConfig.enabled()).isFalse();
    assertThat(queueConfig.host()).isEqualTo("amqp.test.local");
    assertThat(queueConfig.port()).isEqualTo(5673);
    assertThat(queueConfig.inboxQueueName()).isEqualTo("njall.inbox.activities");

    var queueService = injector.getInstance(AmqpQueueService.class);
    assertThat(injector.getInstance(AmqpClient.class)).isSameAs(queueService);
    assertThat(queueService.isConnected()).isFalse();

    var mapper = injector.getInstance(ObjectMapper.class);
    var publisher = injector.getInstance(InboxActivityPublisher.class);
    var envelope =
        new InboxQueueEnvelope(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            ImmutableList.of(UUID.randomUUID()),
            Instant.parse("2026-11-14T12:00:00Z"),
            mapper.createObjectNode().put("id", "https://larpconnect.app/act/1"));
    assertThat(publisher.publish(envelope)).isCompletedExceptionally();

    var healthCheck = injector.getInstance(AmqpHealthCheck.class);
    assertThat(healthCheck.check().isHealthy()).isTrue();

    var healthChecks = injector.getInstance(Key.get(new TypeLiteral<Set<HealthCheck>>() {}));
    assertThat(healthChecks).contains(healthCheck);
  }

  @Test
  @DisplayName("Overriding AmqpQueueService redirects AmqpClient via two-interface binding")
  void queueModule_overrideQueueService_redirectsAmqpClient() {
    var fakeService = new FakeAmqpQueueService();
    var overriddenInjector =
        Guice.createInjector(
            Modules.override(new QueueModule())
                .with(
                    new AbstractModule() {
                      @Override
                      protected void configure() {
                        bind(AmqpQueueService.class).toInstance(fakeService);
                      }
                    }),
            createBaseTestModule());
    assertThat(overriddenInjector.getInstance(AmqpClient.class)).isSameAs(fakeService);
  }
}
