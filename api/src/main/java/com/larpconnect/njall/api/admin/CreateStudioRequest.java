package com.larpconnect.njall.api.admin;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Request payload for creating a new studio. */
public record CreateStudioRequest(@JsonProperty("alias") String alias) {

  public CreateStudioRequest {
    requireNonNull(alias, "alias cannot be null");
  }

  /**
   * Pure factory method creating a CreateStudioRequest.
   *
   * @param alias The studio alias.
   * @return A new instance of CreateStudioRequest.
   */
  public static CreateStudioRequest of(String alias) {
    return new CreateStudioRequest(alias);
  }
}
