package com.larpconnect.njall.api.verticle;

import com.larpconnect.njall.common.annotations.AiContract;

/**
 * Standard responses that indicate what the underlying verticle should do next after processing a
 * message.
 *
 * <p>By abstracting the control flow (such as shutting down the consumer or acknowledging the
 * message) away from the business logic handling the message, this allows verticles to remain
 * focused on handling events without needing to directly manipulate the Vert.x EventBus.
 */
@AiContract(
    implementationHint =
        "Standard responses indicating what the underlying verticle should do next "
            + "after processing a message.")
public enum BasicResponse implements MessageResponse {
  /** Indicates that the verticle should continue processing messages on this channel. */
  CONTINUE,

  /** Indicates that the verticle should unregister its consumer from the EventBus. */
  SHUTDOWN
}
