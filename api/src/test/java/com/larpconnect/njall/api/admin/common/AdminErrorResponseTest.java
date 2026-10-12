package com.larpconnect.njall.api.admin.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class AdminErrorResponseTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("constructor with StatusCode sets status, code, and message")
  void constructor_withStatusCode() {
    var response = new AdminErrorResponse(StatusCodes.NOT_FOUND, "Not found");

    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
    assertThat(response.code()).isEqualTo(404);
    assertThat(response.message()).isEqualTo("Not found");
  }

  @Test
  @DisplayName("serializes to JSON with code and message, omitting status object")
  void serialize_producesExpectedJson() throws Exception {
    var response = new AdminErrorResponse(StatusCodes.BAD_REQUEST, "Invalid format");
    var json = objectMapper.writeValueAsString(response);

    assertThat(json).contains("\"code\":400");
    assertThat(json).contains("\"message\":\"Invalid format\"");
    assertThat(json).doesNotContain("\"status\"");
  }

  @Test
  @DisplayName("deserializes from JSON with code and message into AdminErrorResponse")
  void deserialize_parsesExpectedResponse() throws Exception {
    var json = "{\"code\":409,\"message\":\"Already exists\"}";
    var response = objectMapper.readValue(json, AdminErrorResponse.class);

    assertThat(response.status()).isEqualTo(StatusCodes.CONFLICT);
    assertThat(response.code()).isEqualTo(409);
    assertThat(response.message()).isEqualTo("Already exists");
  }
}
