package com.larpconnect.njall.api.verticle;

/**
 * A sealed interface representing the outcome of handling a message over the Vert.x EventBus.
 *
 * <p>By requiring handlers to return a structured response rather than interacting with the message
 * directly, the framework abstracts control flow (such as replying or unregistering a consumer)
 * away from the business logic. This decouples verticles from the underlying EventBus mechanics,
 * making handlers easier to test and preventing accidental misuse of the raw Vert.x APIs.
 */
public sealed interface MessageResponse permits BasicResponse, ReplyResponse {}
