package com.larpconnect.njall.data.domain;

import com.google.errorprone.annotations.Immutable;
import java.util.UUID;

/**
 * Immutable domain representation of a federated actor endpoint.
 *
 * @param actorId The actor UUID identifier.
 * @param name The endpoint identifier name (alphanumeric, length < 32).
 * @param endpoint The endpoint URI target.
 */
@Immutable
public record AgentEndpoint(UUID actorId, String name, String endpoint) {}
