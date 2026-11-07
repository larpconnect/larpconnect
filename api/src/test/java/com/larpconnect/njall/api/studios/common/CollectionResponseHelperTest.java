package com.larpconnect.njall.api.studios.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.domain.Collection;
import com.larpconnect.njall.data.domain.ExpandedEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class CollectionResponseHelperTest {

  private final UUID collectionId = UUID.randomUUID();
  private final UUID owner = UUID.randomUUID();
  private final UUID ownerId = UUID.randomUUID();
  private final Instant now = Instant.now();
  private static final String STUDIO_IDENTIFIER = "valiant";

  @Test
  @DisplayName("toCompressedResponse maps collection to compressed OrderedCollectionResponse")
  void toCompressedResponse_mapsCorrectly() {
    var itemId = UUID.randomUUID();
    var collection =
        new Collection(
            collectionId,
            owner,
            ownerId,
            "Featured Cast",
            Optional.of("Cast summary"),
            1,
            ImmutableList.of(itemId),
            now,
            now,
            Optional.empty());

    var response = CollectionResponseHelper.toCompressedResponse(STUDIO_IDENTIFIER, collection);

    assertThat(response.id()).isEqualTo(collectionId);
    assertThat(response.type()).isEqualTo("OrderedCollection");
    assertThat(response.name()).isEqualTo("Featured Cast");
    assertThat(response.summary()).contains("Cast summary");
    assertThat(response.generator()).isEqualTo("/api/studios/valiant/v1/actors/" + owner);
    assertThat(response.ownerId()).isEqualTo(ownerId);
    assertThat(response.totalItems()).isEqualTo(1);
    assertThat(response.orderedItems()).containsExactly(itemId);
  }

  @Test
  @DisplayName("toExpandedResponse maps collection and items to expanded OrderedCollectionResponse")
  void toExpandedResponse_mapsCorrectly() {
    var collection =
        new Collection(
            collectionId,
            owner,
            ownerId,
            "Featured Cast",
            Optional.of("Cast summary"),
            0,
            ImmutableList.of(),
            now,
            now,
            Optional.empty());

    var itemId = UUID.randomUUID();
    var itemOwner = UUID.randomUUID();
    var expandedEntity =
        new ExpandedEntity(
            itemId, itemOwner, "Individual", Optional.of("Alice"), Optional.of("Heroine"));

    var response =
        CollectionResponseHelper.toExpandedResponse(
            STUDIO_IDENTIFIER, collection, ImmutableList.of(expandedEntity));

    assertThat(response.id()).isEqualTo(collectionId);
    assertThat(response.totalItems()).isEqualTo(1);
    assertThat(response.orderedItems()).hasSize(1);
    var item = (ExpandedItemResponse) response.orderedItems().get(0);
    assertThat(item.id()).isEqualTo(itemId);
    assertThat(item.type()).isEqualTo("Individual");
    assertThat(item.generator()).isEqualTo("/api/studios/valiant/v1/actors/" + itemOwner);
    assertThat(item.name()).contains("Alice");
    assertThat(item.summary()).contains("Heroine");
  }
}
