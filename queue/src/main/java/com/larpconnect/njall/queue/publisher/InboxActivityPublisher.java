package com.larpconnect.njall.queue.publisher;

import java.util.concurrent.CompletableFuture;

/** Publisher service dispatching FeatherPub Activity envelopes over AMQP 1.0. */
public interface InboxActivityPublisher {

  /**
   * Publishes an accepted Activity envelope to the AMQP 1.0 inbox queue.
   *
   * @param envelope Ingress activity envelope to publish.
   * @return A CompletableFuture completing when the broker acknowledges ACCEPTED disposition, or
   *     completing exceptionally with {@link QueuePublishException} on error or rejection.
   */
  CompletableFuture<Void> publish(InboxQueueEnvelope envelope);
}
