package com.larpconnect.njall.api.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Structured AIP-193 error response representation. */
public record AdminErrorResponse(
    @JsonProperty("code") int code, @JsonProperty("message") String message) {

  /**
   * Pure factory method constructing an AdminErrorResponse.
   *
   * @param code The HTTP status code.
   * @param message Descriptive error message.
   * @return New instance of AdminErrorResponse.
   */
  public static AdminErrorResponse of(int code, String message) {
    return new AdminErrorResponse(code, message);
  }
}
