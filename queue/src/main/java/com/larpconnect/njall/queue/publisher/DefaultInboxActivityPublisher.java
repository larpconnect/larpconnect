package com.larpconnect.njall.queue.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.larpconnect.njall.queue.config.QueueConfig;
import com.rabbitmq.client.amqp.Message;
import com.rabbitmq.client.amqp.Publisher;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link InboxActivityPublisher} publishing JSON messages over AMQP 1.0.
 */
final class DefaultInboxActivityPublisher implements InboxActivityPublisher {

  private static final String CONTENT_TYPE_JSON = "application/json";
  private static final String PROPERTY_TRACEPARENT = "traceparent";

  private final Logger logger = LoggerFactory.getLogger(DefaultInboxActivityPublisher.class);

  private final AmqpClient amqpClient;
  private final QueueConfig config;
  private final ObjectMapper objectMapper;

  @Inject
  DefaultInboxActivityPublisher(
      AmqpClient amqpClient, QueueConfig config, ObjectMapper objectMapper) {
    this.amqpClient = amqpClient;
    this.config = config;
    this.objectMapper = objectMapper;
  }

  @Override
  public CompletableFuture<Void> publish(InboxQueueEnvelope envelope) {
    if (!amqpClient.isRunning()) {
      return CompletableFuture.failedFuture(
          createPublishException("AMQP queue service is not running"));
    }

    var publisher = amqpClient.publisher();
    if (publisher == null) {
      return CompletableFuture.failedFuture(
          createPublishException("AMQP publisher is unavailable"));
    }

    try {
      var bytes = objectMapper.writeValueAsBytes(envelope);
      var message = createAmqpMessage(publisher, envelope, bytes);
      return dispatchMessage(publisher, message);
    } catch (JsonProcessingException ex) {
      logger.error("Failed to serialize activity JSON for publishing: {}", ex.getMessage());
      return CompletableFuture.failedFuture(
          createPublishException("Failed to serialize activity JSON payload", ex));
    } catch (RuntimeException ex) {
      amqpClient.invalidatePublisher(publisher);
      logger.error("Failed to initiate AMQP message publish: {}", ex.getMessage());
      return CompletableFuture.failedFuture(
          createPublishException("Failed to publish message to AMQP queue", ex));
    }
  }

  private Message createAmqpMessage(
      Publisher publisher, InboxQueueEnvelope envelope, byte[] payloadBytes) {
    var message = publisher.message(payloadBytes);
    message.durable(true);
    message.contentType(CONTENT_TYPE_JSON);
    message.to(config.inboxQueueName());

    var activityId = envelope.activity().path("id").asText();
    if (!activityId.isBlank()) {
      message.messageId(activityId);
      logger.debug("Prepared activity {} for queue {}", activityId, config.inboxQueueName());
    }

    envelope.traceparent().ifPresent(tp -> message.property(PROPERTY_TRACEPARENT, tp));
    return message;
  }

  private CompletableFuture<Void> dispatchMessage(Publisher publisher, Message message) {
    var future = createVoidFuture();
    publisher.publish(
        message,
        context -> {
          if (context.status() == Publisher.Status.ACCEPTED) {
            future.complete(null);
          } else {
            var cause = context.failureCause();
            var errorMsg = "Broker rejected message with status: " + context.status();
            if (cause != null) {
              amqpClient.invalidatePublisher(publisher);
              future.completeExceptionally(createPublishException(errorMsg, cause));
            } else {
              future.completeExceptionally(createPublishException(errorMsg));
            }
          }
        });
    return future;
  }

  private static CompletableFuture<Void> createVoidFuture() {
    return new CompletableFuture<>();
  }

  private static QueuePublishException createPublishException(String message) {
    return new QueuePublishException(message);
  }

  private static QueuePublishException createPublishException(String message, Throwable cause) {
    return new QueuePublishException(message, cause);
  }
}
