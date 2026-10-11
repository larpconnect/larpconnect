package com.larpconnect.njall.data.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class IndividualDomainTest {

  @Test
  @DisplayName("Canonical constructor retains all fields and computes isDeleted")
  void canonicalConstructor_retainsFields() {
    var id = UUID.randomUUID();
    var now = Instant.now();

    var individual =
        new Individual(
            id, "Jane Eyre", Optional.of("Visiting scholar"), now, now, Optional.empty());

    assertThat(individual.id()).isEqualTo(id);
    assertThat(individual.name()).isEqualTo("Jane Eyre");
    assertThat(individual.summary()).contains("Visiting scholar");
    assertThat(individual.createdOn()).isEqualTo(now);
    assertThat(individual.updatedOn()).isEqualTo(now);
    assertThat(individual.deletedOn()).isEmpty();
    assertThat(individual.isDeleted()).isFalse();
  }

  @Test
  @DisplayName("Nullable constructor wraps nulls into empty Optionals")
  void nullableConstructor_wrapsNulls() {
    var id = UUID.randomUUID();
    var now = Instant.now();

    var individual =
        new Individual(id, "Edward Rochester", (String) null, now, now, (Instant) null);

    assertThat(individual.id()).isEqualTo(id);
    assertThat(individual.name()).isEqualTo("Edward Rochester");
    assertThat(individual.summary()).isEmpty();
    assertThat(individual.deletedOn()).isEmpty();
    assertThat(individual.isDeleted()).isFalse();
  }

  @Test
  @DisplayName("Nullable constructor wraps present non-nulls into present Optionals")
  void nullableConstructor_wrapsNonNulls() {
    var id = UUID.randomUUID();
    var now = Instant.now();

    var individual = new Individual(id, "Edward Rochester", "Master of Thornfield", now, now, now);

    assertThat(individual.id()).isEqualTo(id);
    assertThat(individual.name()).isEqualTo("Edward Rochester");
    assertThat(individual.summary()).contains("Master of Thornfield");
    assertThat(individual.deletedOn()).contains(now);
    assertThat(individual.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("isDeleted returns true when deletedOn is present")
  void isDeleted_returnsTrueWhenDeletedOnPresent() {
    var id = UUID.randomUUID();
    var now = Instant.now();

    var individual = new Individual(id, "Bertha Mason", null, now, now, now);

    assertThat(individual.isDeleted()).isTrue();
    assertThat(individual.deletedOn()).contains(now);
  }
}
