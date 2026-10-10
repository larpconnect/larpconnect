package com.larpconnect.njall.data.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class HashtagTest {

  @Test
  @DisplayName("Hashtag record exposes properties and isDeleted helper correctly")
  void hashtagRecord_propertiesAndHelpers() {
    var id = UUID.randomUUID();
    var now = Instant.now();

    var activeTag =
        new Hashtag(
            id,
            "SolarPunk",
            "hashtag",
            "/api/studios/valiant/v1/tags/SolarPunk",
            "application/json",
            "Aesthetic",
            now,
            now,
            null);

    assertThat(activeTag.id()).isEqualTo(id);
    assertThat(activeTag.tag()).isEqualTo("SolarPunk");
    assertThat(activeTag.linkType()).isEqualTo("hashtag");
    assertThat(activeTag.url()).isEqualTo("/api/studios/valiant/v1/tags/SolarPunk");
    assertThat(activeTag.mediaType()).isEqualTo("application/json");
    assertThat(activeTag.summary()).contains("Aesthetic");
    assertThat(activeTag.createdOn()).isEqualTo(now);
    assertThat(activeTag.updatedOn()).isEqualTo(now);
    assertThat(activeTag.deletedOn()).isEmpty();
    assertThat(activeTag.isDeleted()).isFalse();

    var deletedTag =
        new Hashtag(
            id,
            "SolarPunk",
            "hashtag",
            "/api/studios/valiant/v1/tags/SolarPunk",
            "application/json",
            Optional.empty(),
            now,
            now,
            Optional.of(now));

    assertThat(deletedTag.summary()).isEmpty();
    assertThat(deletedTag.isDeleted()).isTrue();
  }
}
