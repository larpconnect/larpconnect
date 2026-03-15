package com.larpconnect.njall.api.verticle;

import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.common.annotations.AiContract;
import javax.annotation.ParametersAreNonnullByDefault;

/** Indicates that the verticle should reply to the sender with the specified payload. */
@Immutable
@ParametersAreNonnullByDefault
@AiContract(
    implementationHint =
        "Indicates that the verticle should reply to the sender with the specified payload.")
public record ReplyResponse(
    @SuppressWarnings("Immutable") // The caller specifies the payload which determines mutability
        Object payload)
    implements MessageResponse {}
