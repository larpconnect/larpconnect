package com.larpconnect.njall.api.studios.tags;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class TagValidationTest {

  @Test
  @DisplayName("tryParseUuid correctly parses valid UUID and returns empty for invalid")
  void tryParseUuid_validAndInvalid() {
    var validUuid = UUID.randomUUID();
    assertThat(TagValidation.tryParseUuid(validUuid.toString())).contains(validUuid);
    assertThat(TagValidation.tryParseUuid("not-a-uuid")).isEmpty();
    assertThat(TagValidation.tryParseUuid(null)).isEmpty();
  }

  @Test
  @DisplayName("sanitizeTag strips leading '#', trims, and handles null")
  void sanitizeTag_stripsHashAndTrims() {
    assertThat(TagValidation.sanitizeTag(null)).isEmpty();
    assertThat(TagValidation.sanitizeTag("  #SolarPunk  ")).isEqualTo("SolarPunk");
    assertThat(TagValidation.sanitizeTag("SolarPunk")).isEqualTo("SolarPunk");
    assertThat(TagValidation.sanitizeTag("###SolarPunk")).isEqualTo("##SolarPunk");
  }

  @Test
  @DisplayName("validateTagName accepts valid web-safe Unicode and rejects invalid")
  void validateTagName_scenarios() {
    assertThat(TagValidation.validateTagName("SolarPunk")).isEmpty();
    assertThat(TagValidation.validateTagName("#cyber-punk_2077")).isEmpty();
    assertThat(TagValidation.validateTagName("")).contains("tag must not be blank");
    assertThat(TagValidation.validateTagName("   ")).contains("tag must not be blank");
    assertThat(TagValidation.validateTagName("#")).contains("tag must not be blank");

    var longTag = "a".repeat(33);
    assertThat(TagValidation.validateTagName(longTag))
        .contains("tag must not exceed 32 characters in length");

    assertThat(TagValidation.validateTagName("has space"))
        .contains(
            "tag must contain only web-safe Unicode letters, digits, underscores, or hyphens");
    assertThat(TagValidation.validateTagName("tag!special"))
        .contains(
            "tag must contain only web-safe Unicode letters, digits, underscores, or hyphens");
  }

  @Test
  @DisplayName("validateCreate validates tag property")
  void validateCreate_scenarios() {
    assertThat(TagValidation.validateCreate(new CreateTagRequest("validTag"))).isEmpty();
    assertThat(TagValidation.validateCreate(new CreateTagRequest(""))).isPresent();
  }

  @Test
  @DisplayName("validateBatchCreate validates batch size and items")
  void validateBatchCreate_scenarios() {
    var emptyBatch = new BatchCreateTagsRequest(ImmutableList.of());
    assertThat(TagValidation.validateBatchCreate(emptyBatch))
        .contains("requests must not be empty");

    var items = ImmutableList.<CreateTagRequest>builder();
    for (int i = 0; i < 51; i++) {
      items.add(new CreateTagRequest("tag" + i));
    }
    assertThat(TagValidation.validateBatchCreate(new BatchCreateTagsRequest(items.build())))
        .contains("requests array must not exceed 50 items");

    var invalidItemBatch =
        new BatchCreateTagsRequest(
            ImmutableList.of(new CreateTagRequest("valid"), new CreateTagRequest("")));
    assertThat(TagValidation.validateBatchCreate(invalidItemBatch)).isPresent();

    var validBatch =
        new BatchCreateTagsRequest(
            ImmutableList.of(new CreateTagRequest("alpha"), new CreateTagRequest("beta")));
    assertThat(TagValidation.validateBatchCreate(validBatch)).isEmpty();
  }

  @Test
  @DisplayName("validateUpdate validates empty payload and tag field")
  void validateUpdate_scenarios() {
    var emptyUpdate = new UpdateTagRequest(Optional.empty(), Optional.empty());
    assertThat(TagValidation.validateUpdate(emptyUpdate))
        .contains("At least one field (tag or summary) must be specified for update");

    var summaryOnly = new UpdateTagRequest(Optional.empty(), Optional.of("new summary"));
    assertThat(TagValidation.validateUpdate(summaryOnly)).isEmpty();

    var validTagUpdate = new UpdateTagRequest(Optional.of("newTag"), Optional.empty());
    assertThat(TagValidation.validateUpdate(validTagUpdate)).isEmpty();

    var invalidTagUpdate = new UpdateTagRequest(Optional.of("bad tag!"), Optional.empty());
    assertThat(TagValidation.validateUpdate(invalidTagUpdate)).isPresent();
  }

  @Test
  @DisplayName("BatchCreateTagsRequest handles null requests via JsonCreator")
  void batchCreateTagsRequest_nullRequests() {
    var req = new BatchCreateTagsRequest(null);
    assertThat(req.requests()).isEmpty();
  }

  @Test
  @DisplayName("BatchCreateTagsResponse handles null and non-null tags via JsonCreator")
  void batchCreateTagsResponse_jsonCreator() {
    var resNull = new BatchCreateTagsResponse(null);
    assertThat(resNull.tags()).isEmpty();

    var tag =
        new TagResponse(
            UUID.randomUUID(),
            "SolarPunk",
            "hashtag",
            "/url",
            "application/json",
            Optional.empty(),
            Instant.now(),
            Instant.now());
    var resList = new BatchCreateTagsResponse(ImmutableList.of(tag));
    assertThat(resList.tags()).hasSize(1);
  }
}
