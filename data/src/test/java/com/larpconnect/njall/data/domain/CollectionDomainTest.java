package com.larpconnect.njall.data.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class CollectionDomainTest {

  @Test
  @DisplayName("Collection canonical constructor retains all fields and computes isDeleted")
  void collection_canonicalConstructor_retainsFields() {
    var id = UUID.randomUUID();
    var ownerId = UUID.randomUUID();
    var itemId = UUID.randomUUID();
    var now = Instant.now();

    var collection =
        new Collection(
            id,
            ownerId,
            "Featured Cast",
            Optional.of("Primary actors"),
            1,
            ImmutableList.of(itemId),
            now,
            now,
            Optional.empty());

    assertThat(collection.id()).isEqualTo(id);
    assertThat(collection.ownerId()).isEqualTo(ownerId);
    assertThat(collection.name()).isEqualTo("Featured Cast");
    assertThat(collection.summary()).contains("Primary actors");
    assertThat(collection.totalItems()).isEqualTo(1);
    assertThat(collection.itemIds()).containsExactly(itemId);
    assertThat(collection.createdOn()).isEqualTo(now);
    assertThat(collection.updatedOn()).isEqualTo(now);
    assertThat(collection.deletedOn()).isEmpty();
    assertThat(collection.isDeleted()).isFalse();
  }

  @Test
  @DisplayName("Collection nullable constructor wraps nulls into empty Optionals")
  void collection_nullableConstructor_wrapsNulls() {
    var id = UUID.randomUUID();
    var ownerId = UUID.randomUUID();
    var now = Instant.now();

    var collection =
        new Collection(id, ownerId, "Crew", (String) null, 0, List.of(), now, now, (Instant) null);

    assertThat(collection.id()).isEqualTo(id);
    assertThat(collection.ownerId()).isEqualTo(ownerId);
    assertThat(collection.name()).isEqualTo("Crew");
    assertThat(collection.summary()).isEmpty();
    assertThat(collection.totalItems()).isEqualTo(0);
    assertThat(collection.itemIds()).isEmpty();
    assertThat(collection.deletedOn()).isEmpty();
    assertThat(collection.isDeleted()).isFalse();
  }

  @Test
  @DisplayName("Collection isDeleted returns true when deletedOn is present")
  void collection_isDeleted_returnsTrueWhenDeletedOnPresent() {
    var id = UUID.randomUUID();
    var ownerId = UUID.randomUUID();
    var now = Instant.now();

    var collection =
        new Collection(id, ownerId, "Deleted Collection", null, 0, List.of(), now, now, now);

    assertThat(collection.isDeleted()).isTrue();
    assertThat(collection.deletedOn()).contains(now);
  }

  @Test
  @DisplayName("CollectionItem record retains all fields")
  void collectionItem_retainsFields() {
    var id = UUID.randomUUID();
    var collectionId = UUID.randomUUID();
    var refersToTenantId = UUID.randomUUID();
    var refersToId = UUID.randomUUID();
    var now = Instant.now();

    var item = new CollectionItem(id, collectionId, 0, now, refersToTenantId, refersToId);

    assertThat(item.id()).isEqualTo(id);
    assertThat(item.collectionId()).isEqualTo(collectionId);
    assertThat(item.ordinal()).isEqualTo(0);
    assertThat(item.addedOn()).isEqualTo(now);
    assertThat(item.refersToTenantId()).isEqualTo(refersToTenantId);
    assertThat(item.refersToId()).isEqualTo(refersToId);
  }

  @Test
  @DisplayName("ExpandedEntity record retains fields and handles nulls")
  void expandedEntity_retainsFields() {
    var id = UUID.randomUUID();

    var entity = new ExpandedEntity(id, "Individual", "Alice", "Lead");
    assertThat(entity.id()).isEqualTo(id);
    assertThat(entity.type()).isEqualTo("Individual");
    assertThat(entity.name()).contains("Alice");
    assertThat(entity.summary()).contains("Lead");

    var entityWithoutNulls = new ExpandedEntity(id, "Event", (String) null, (String) null);
    assertThat(entityWithoutNulls.name()).isEmpty();
    assertThat(entityWithoutNulls.summary()).isEmpty();
  }
}
