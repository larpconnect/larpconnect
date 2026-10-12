package com.larpconnect.njall.api.studios.actors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;

/** Request payload for adding or updating a federated actor protocol endpoint. */
@Immutable
public record AddEndpointRequest(
    @JsonProperty("name") String name, @JsonProperty("endpoint") String endpoint) {}
