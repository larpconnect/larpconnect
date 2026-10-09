package com.larpconnect.njall.api.studios.links;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class LinkValidationAndDtoTest {

  private final ObjectMapper objectMapper =
      new ObjectMapper().registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());

  @Test
  @DisplayName("LinkValidation handles UUID parsing")
  void linkValidation_tryParseUuid() {
    var uuid = UUID.randomUUID();
    assertThat(LinkValidation.tryParseUuid(uuid.toString())).contains(uuid);
    assertThat(LinkValidation.tryParseUuid("not-a-uuid")).isEmpty();
    assertThat(LinkValidation.tryParseUuid(null)).isEmpty();
  }

  @Test
  @DisplayName("LinkValidation handles URI validation")
  void linkValidation_isValidUri() {
    assertThat(LinkValidation.isValidUri("https://example.com")).isTrue();
    assertThat(LinkValidation.isValidUri("mailto:user@example.com")).isTrue();
    assertThat(LinkValidation.isValidUri("")).isFalse();
    assertThat(LinkValidation.isValidUri(null)).isFalse();
    assertThat(LinkValidation.isValidUri("not a uri")).isFalse();
    assertThat(LinkValidation.isValidUri("relative/path")).isFalse();
  }

  @Test
  @DisplayName("LinkValidation validates CreateLinkRequest")
  void linkValidation_validateCreate() {
    var emptyType = new CreateLinkRequest("", "https://example.com");
    assertThat(LinkValidation.validateCreate(emptyType)).contains("linkType must not be blank");

    var blankType = new CreateLinkRequest("   ", "https://example.com");
    assertThat(LinkValidation.validateCreate(blankType)).contains("linkType must not be blank");

    var emptyUrl = new CreateLinkRequest("website", "");
    assertThat(LinkValidation.validateCreate(emptyUrl)).contains("url must be a valid URI");

    var invalidUrl = new CreateLinkRequest("website", "not a uri");
    assertThat(LinkValidation.validateCreate(invalidUrl)).contains("url must be a valid URI");

    var blankMediaType = new CreateLinkRequest("website", "https://example.com", "   ", "summary");
    assertThat(LinkValidation.validateCreate(blankMediaType))
        .contains("mediaType must not be blank if provided");

    var valid = new CreateLinkRequest("website", "https://example.com", "text/html", "summary");
    assertThat(LinkValidation.validateCreate(valid)).isEmpty();
  }

  @Test
  @DisplayName("LinkValidation validates UpdateLinkRequest")
  void linkValidation_validateUpdate() {
    var blankType = new UpdateLinkRequest("  ", null, null, null);
    assertThat(LinkValidation.validateUpdate(blankType)).contains("linkType must not be blank");

    var invalidUrl = new UpdateLinkRequest(null, "invalid-uri", null, null);
    assertThat(LinkValidation.validateUpdate(invalidUrl)).contains("url must be a valid URI");

    var blankMediaType = new UpdateLinkRequest(null, null, "   ", null);
    assertThat(LinkValidation.validateUpdate(blankMediaType))
        .contains("mediaType must not be blank");

    var valid = new UpdateLinkRequest("website", "https://example.com", "text/html", "summary");
    assertThat(LinkValidation.validateUpdate(valid)).isEmpty();
  }

  @Test
  @DisplayName("CreateLinkRequest and UpdateLinkRequest serialize and deserialize cleanly")
  void dtoSerialization() throws Exception {
    var createJson =
        """
        {
          "linkType": "website",
          "url": "https://valiant.example.com",
          "mediaType": "text/html",
          "summary": "Homepage"
        }
        """;
    var createReq = objectMapper.readValue(createJson, CreateLinkRequest.class);
    assertThat(createReq.linkType()).isEqualTo("website");
    assertThat(createReq.url()).isEqualTo("https://valiant.example.com");
    assertThat(createReq.mediaType()).contains("text/html");
    assertThat(createReq.summary()).contains("Homepage");

    var updateJson =
        """
        {
          "url": "https://updated.example.com"
        }
        """;
    var updateReq = objectMapper.readValue(updateJson, UpdateLinkRequest.class);
    assertThat(updateReq.url()).contains("https://updated.example.com");
    assertThat(updateReq.linkType()).isEmpty();
  }

  @Test
  @DisplayName("LinkResponse constructs and serializes correctly")
  void linkResponse_serialization() throws Exception {
    var linkId = UUID.randomUUID();
    var now = Instant.now();
    var response =
        new LinkResponse(
            linkId,
            "website",
            "https://valiant.example.com",
            "text/html",
            Optional.of("Official Studio"),
            now,
            now);

    assertThat(response.id()).isEqualTo(linkId);
    assertThat(response.linkType()).isEqualTo("website");
    assertThat(response.url()).isEqualTo("https://valiant.example.com");
    assertThat(response.mediaType()).isEqualTo("text/html");
    assertThat(response.summary()).contains("Official Studio");
    assertThat(response.createdOn()).isEqualTo(now);
    assertThat(response.updatedOn()).isEqualTo(now);

    var json = objectMapper.writeValueAsString(response);
    assertThat(json).contains(linkId.toString());
    assertThat(json).doesNotContain("tenantId");
    assertThat(json).doesNotContain("deletedOn");
  }
}
