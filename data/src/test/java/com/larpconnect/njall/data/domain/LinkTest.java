package com.larpconnect.njall.data.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class LinkTest {

  @Test
  @DisplayName("Link record exposes properties and isDeleted helper correctly")
  void linkRecord_propertiesAndHelpers() {
    var id = UUID.randomUUID();
    var now = Instant.now();

    var activeLink =
        new Link(id, "website", "https://example.com", "text/html", "Homepage", now, now, null);

    assertThat(activeLink.id()).isEqualTo(id);
    assertThat(activeLink.linkType()).isEqualTo("website");
    assertThat(activeLink.url()).isEqualTo("https://example.com");
    assertThat(activeLink.mediaType()).isEqualTo("text/html");
    assertThat(activeLink.summary()).contains("Homepage");
    assertThat(activeLink.createdOn()).isEqualTo(now);
    assertThat(activeLink.updatedOn()).isEqualTo(now);
    assertThat(activeLink.deletedOn()).isEmpty();
    assertThat(activeLink.isDeleted()).isFalse();

    var deletedLink =
        new Link(
            id,
            "website",
            "https://example.com",
            "text/html",
            Optional.empty(),
            now,
            now,
            Optional.of(now));

    assertThat(deletedLink.summary()).isEmpty();
    assertThat(deletedLink.isDeleted()).isTrue();
  }
}
