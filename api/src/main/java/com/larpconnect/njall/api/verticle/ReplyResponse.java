package com.larpconnect.njall.api.verticle;

import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.common.annotations.AiContract;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Indicates that the verticle should reply to the sender with the specified payload.
 *
 * <p>By encapsulating the reply payload within this record, handlers can cleanly signal their
 * intention to respond to a request without directly interacting with the Vert.x EventBus {@code
 * Message} object. This decoupling simplifies testing, as handlers can be evaluated purely on their
 * return values rather than their side effects, and prevents business logic from becoming entangled
 * with low-level messaging infrastructure.
 */
@Immutable
@ParametersAreNonnullByDefault
@AiContract(
    implementationHint =
        "Indicates that the verticle should reply to the sender with the specified payload.")
public record ReplyResponse(
    @SuppressWarnings("Immutable") // The caller specifies the payload which determines mutability
        Object payload)
    implements MessageResponse {}
