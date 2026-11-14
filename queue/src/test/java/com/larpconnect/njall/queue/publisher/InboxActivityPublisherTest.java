package com.larpconnect.njall.queue.publisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.queue.config.QueueConfig;
import com.rabbitmq.client.amqp.Message;
import com.rabbitmq.client.amqp.Publisher;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class InboxActivityPublisherTest {

  private final ObjectMapper objectMapper =
      new ObjectMapper().registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());

  private AmqpClient amqpClient;
  private QueueConfig queueConfig;
  private Publisher publisher;
  private Message message;
  private DefaultInboxActivityPublisher activityPublisher;

  @BeforeEach
  void setUp() {
    publisher = mock(Publisher.class);
    message = mock(Message.class);
    amqpClient = mock(AmqpClient.class);

    when(amqpClient.isRunning()).thenReturn(true);
    when(amqpClient.publisher()).thenReturn(publisher);
    when(publisher.message(any(byte[].class))).thenReturn(message);

    queueConfig =
        new QueueConfig(true, "localhost", 5672, "njall_amqp", "secret", "njall.inbox.activities");

    activityPublisher = new DefaultInboxActivityPublisher(amqpClient, queueConfig, objectMapper);
  }

  @Test
  @DisplayName("publish fails future when queue service is not running")
  void publish_serviceNotRunning_returnsFailedFuture() {
    when(amqpClient.isRunning()).thenReturn(false);

    var envelope = createTestEnvelope("https://remote.example/act/1", Optional.empty());
    var future = activityPublisher.publish(envelope);

    assertThat(future).isCompletedExceptionally();
    assertThatThrownBy(future::get)
        .isInstanceOf(ExecutionException.class)
        .hasCauseInstanceOf(QueuePublishException.class)
        .hasMessageContaining("AMQP queue service is not running");
  }

  @Test
  @DisplayName("publish fails future when publisher is null")
  void publish_nullPublisher_returnsFailedFuture() {
    when(amqpClient.publisher()).thenReturn(null);

    var envelope = createTestEnvelope("https://remote.example/act/2", Optional.empty());
    var future = activityPublisher.publish(envelope);

    assertThat(future).isCompletedExceptionally();
    assertThatThrownBy(future::get)
        .isInstanceOf(ExecutionException.class)
        .hasCauseInstanceOf(QueuePublishException.class)
        .hasMessageContaining("AMQP publisher is unavailable");
  }

  @Test
  @DisplayName("publish completes successfully when broker confirms ACCEPTED disposition")
  void publish_acceptedDisposition_completesNormally() throws Exception {
    var traceparent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";
    var activityId = "https://remote.example/activities/accepted-001";
    var envelope = createTestEnvelope(activityId, Optional.of(traceparent));

    doAnswer(
            invocation -> {
              Publisher.Callback callback = invocation.getArgument(1);
              var context = mock(Publisher.Context.class);
              when(context.status()).thenReturn(Publisher.Status.ACCEPTED);
              callback.handle(context);
              return null;
            })
        .when(publisher)
        .publish(any(Message.class), any(Publisher.Callback.class));

    var future = activityPublisher.publish(envelope);

    assertThat(future.get()).isNull();
    verify(message).durable(true);
    verify(message).contentType("application/json");
    verify(message).to("njall.inbox.activities");
    verify(message).messageId(activityId);
    verify(message).property("traceparent", traceparent);
    verify(amqpClient, never()).invalidatePublisher(any());
  }

  @Test
  @DisplayName("publish does not set traceparent when empty in envelope")
  void publish_emptyTraceparent_omitsTraceparentProperty() throws Exception {
    var activityId = "https://remote.example/activities/notp-001";
    var activity = objectMapper.createObjectNode().put("id", activityId).put("type", "Create");
    var envelope =
        new InboxQueueEnvelope(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            ImmutableList.of(UUID.randomUUID()),
            Instant.now(),
            activity);

    doAnswer(
            invocation -> {
              Publisher.Callback callback = invocation.getArgument(1);
              var context = mock(Publisher.Context.class);
              when(context.status()).thenReturn(Publisher.Status.ACCEPTED);
              callback.handle(context);
              return null;
            })
        .when(publisher)
        .publish(any(Message.class), any(Publisher.Callback.class));

    var future = activityPublisher.publish(envelope);

    assertThat(future.get()).isNull();
    verify(message, never()).property(anyString(), anyString());
  }

  @Test
  @DisplayName("publish completes exceptionally without invalidating link on semantic REJECTED")
  void publish_rejectedDisposition_completesExceptionally() {
    var envelope = createTestEnvelope("https://remote.example/act/reject", Optional.empty());

    doAnswer(
            invocation -> {
              Publisher.Callback callback = invocation.getArgument(1);
              var context = mock(Publisher.Context.class);
              when(context.status()).thenReturn(Publisher.Status.REJECTED);
              callback.handle(context);
              return null;
            })
        .when(publisher)
        .publish(any(Message.class), any(Publisher.Callback.class));

    var future = activityPublisher.publish(envelope);

    assertThat(future).isCompletedExceptionally();
    assertThatThrownBy(future::get)
        .isInstanceOf(ExecutionException.class)
        .hasCauseInstanceOf(QueuePublishException.class)
        .hasMessageContaining("Broker rejected message with status: REJECTED");
    verify(amqpClient, never()).invalidatePublisher(any());
  }

  @Test
  @DisplayName("publish invalidates publisher and includes cause when disposition has failureCause")
  void publish_dispositionWithFailureCause_invalidatesPublisherAndIncludesCause() {
    var envelope = createTestEnvelope("https://remote.example/act/failure", Optional.empty());
    var rootCause = new RuntimeException("Broker link closed");

    doAnswer(
            invocation -> {
              Publisher.Callback callback = invocation.getArgument(1);
              var context = mock(Publisher.Context.class);
              when(context.status()).thenReturn(Publisher.Status.RELEASED);
              when(context.failureCause()).thenReturn(rootCause);
              callback.handle(context);
              return null;
            })
        .when(publisher)
        .publish(any(Message.class), any(Publisher.Callback.class));

    var future = activityPublisher.publish(envelope);

    assertThat(future).isCompletedExceptionally();
    assertThatThrownBy(future::get)
        .isInstanceOf(ExecutionException.class)
        .hasCauseInstanceOf(QueuePublishException.class)
        .hasRootCause(rootCause);
    verify(amqpClient).invalidatePublisher(publisher);
  }

  @Test
  @DisplayName("publish completes exceptionally when JSON serialization fails")
  void publish_jsonSerializationThrows_returnsFailedFuture() throws Exception {
    var failingMapper = mock(ObjectMapper.class);
    when(failingMapper.writeValueAsBytes(any()))
        .thenThrow(new JsonGenerationException("Invalid JSON node", (JsonGenerator) null));
    var failingPublisher =
        new DefaultInboxActivityPublisher(amqpClient, queueConfig, failingMapper);

    var envelope = createTestEnvelope("https://remote.example/act/json-err", Optional.empty());
    var future = failingPublisher.publish(envelope);

    assertThat(future).isCompletedExceptionally();
    assertThatThrownBy(future::get)
        .isInstanceOf(ExecutionException.class)
        .hasCauseInstanceOf(QueuePublishException.class)
        .hasMessageContaining("Failed to serialize activity JSON payload");
    verify(amqpClient, never()).invalidatePublisher(any());
  }

  @Test
  @DisplayName(
      "publish invalidates publisher and completes exceptionally when publish throws synchronously")
  void publish_publishThrowsSynchronously_invalidatesAndReturnsFailedFuture() {
    var envelope = createTestEnvelope("https://remote.example/act/sync", Optional.empty());
    doThrow(new RuntimeException("Connection dropped unexpectedly"))
        .when(publisher)
        .publish(any(Message.class), any(Publisher.Callback.class));

    var future = activityPublisher.publish(envelope);

    assertThat(future).isCompletedExceptionally();
    assertThatThrownBy(future::get)
        .isInstanceOf(ExecutionException.class)
        .hasCauseInstanceOf(QueuePublishException.class)
        .hasMessageContaining("Failed to publish message to AMQP queue");
    verify(amqpClient).invalidatePublisher(publisher);
  }

  @Test
  @DisplayName("publish omits messageId when activity has no id property")
  void publish_missingActivityId_omitsMessageId() throws Exception {
    var activity = objectMapper.createObjectNode().put("type", "Create");
    var envelope =
        new InboxQueueEnvelope(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            List.of(UUID.randomUUID()),
            Instant.now(),
            activity,
            Optional.empty());

    doAnswer(
            invocation -> {
              Publisher.Callback callback = invocation.getArgument(1);
              var context = mock(Publisher.Context.class);
              when(context.status()).thenReturn(Publisher.Status.ACCEPTED);
              callback.handle(context);
              return null;
            })
        .when(publisher)
        .publish(any(Message.class), any(Publisher.Callback.class));

    var future = activityPublisher.publish(envelope);

    assertThat(future.get()).isNull();
    verify(message, never()).messageId(anyString());
  }

  private InboxQueueEnvelope createTestEnvelope(String activityId, Optional<String> traceparent) {
    var activity = objectMapper.createObjectNode().put("id", activityId).put("type", "Create");
    return new InboxQueueEnvelope(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        ImmutableList.of(UUID.randomUUID()),
        Instant.now(),
        activity,
        traceparent);
  }
}
