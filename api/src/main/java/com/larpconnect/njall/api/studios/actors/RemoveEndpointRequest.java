package com.larpconnect.njall.api.studios.actors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;

/** Request payload for removing a federated actor protocol endpoint. */
@Immutable
public record RemoveEndpointRequest(@JsonProperty("name") String name) {}
