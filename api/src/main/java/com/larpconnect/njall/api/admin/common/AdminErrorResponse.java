package com.larpconnect.njall.api.admin.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import org.apache.pekko.http.javadsl.model.StatusCode;
import org.apache.pekko.http.javadsl.model.StatusCodes;

/** Structured AIP-193 error response representation. */
@Immutable
public record AdminErrorResponse(
    @JsonIgnore StatusCode status, @JsonProperty("message") String message) {

  @JsonCreator
  public AdminErrorResponse(
      @JsonProperty("code") int code, @JsonProperty("message") String message) {
    this(StatusCodes.get(code), message);
  }

  @JsonProperty("code")
  public int code() {
    return status.intValue();
  }
}
