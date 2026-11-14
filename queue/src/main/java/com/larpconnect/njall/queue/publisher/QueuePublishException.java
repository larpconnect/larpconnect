package com.larpconnect.njall.queue.publisher;

/**
 * Unchecked exception indicating an error occurred while publishing an activity message to AMQP.
 */
public final class QueuePublishException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public QueuePublishException(String message) {
    super(message);
  }

  public QueuePublishException(String message, Throwable cause) {
    super(message, cause);
  }
}
