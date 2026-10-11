package com.larpconnect.njall.api.studios.collections;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.domain.ExpandedEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class CollectionValidationAndDtoTest {

  private final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new Jdk8Module())
          .registerModule(new JavaTimeModule())
          .registerModule(new GuavaModule());

  @Test
  @DisplayName("CollectionValidation handles create and patch rules")
  void collectionValidation_rules() {
    var ownerId = UUID.randomUUID();
    var blankNameCreate = new CreateCollectionRequest(ownerId, "   ");
    assertThat(CollectionValidation.validateCreate(blankNameCreate))
        .contains("Collection name must not be blank");

    var validCreate =
        new CreateCollectionRequest(ownerId, "Featured Cast", Optional.of("Cast summary"));
    assertThat(CollectionValidation.validateCreate(validCreate)).isEmpty();

    var blankPatch = new UpdateCollectionRequest("   ", null);
    assertThat(CollectionValidation.validatePatch(blankPatch))
        .contains("Collection name must not be blank");

    var validPatch = new UpdateCollectionRequest("New Name", "New summary");
    assertThat(CollectionValidation.validatePatch(validPatch)).isEmpty();

    var emptyPatch = new UpdateCollectionRequest(Optional.empty(), Optional.empty());
    assertThat(CollectionValidation.validatePatch(emptyPatch)).isEmpty();
  }

  @Test
  @DisplayName("CollectionValidation handles addItem placement specifiers")
  void collectionValidation_addItem() {
    var refersToId = UUID.randomUUID();
    var beforeId = UUID.randomUUID();

    var validDefault = new AddItemRequest(refersToId);
    assertThat(CollectionValidation.validateAddItem(validDefault)).isEmpty();

    var validBefore = new AddItemRequest(refersToId, Optional.of(beforeId), Optional.empty());
    assertThat(CollectionValidation.validateAddItem(validBefore)).isEmpty();

    var validIndex = new AddItemRequest(refersToId, Optional.empty(), Optional.of(2));
    assertThat(CollectionValidation.validateAddItem(validIndex)).isEmpty();

    var bothSpecifiers = new AddItemRequest(refersToId, Optional.of(beforeId), Optional.of(2));
    assertThat(CollectionValidation.validateAddItem(bothSpecifiers))
        .contains("Cannot specify both 'before' and 'index' placement specifiers");

    var negativeIndex = new AddItemRequest(refersToId, Optional.empty(), Optional.of(-1));
    assertThat(CollectionValidation.validateAddItem(negativeIndex))
        .contains("Index must be non-negative");

    var removeItemReq = new RemoveItemRequest(refersToId);
    assertThat(CollectionValidation.validateRemoveItem(removeItemReq)).isEmpty();
  }

  @Test
  @DisplayName("CollectionValidation checks tryParseUuid")
  void collectionValidation_tryParseUuid() {
    assertThat(CollectionValidation.tryParseUuid(null)).isEmpty();
    assertThat(CollectionValidation.tryParseUuid("")).isEmpty();
    assertThat(CollectionValidation.tryParseUuid("invalid-uuid")).isEmpty();

    var valid = UUID.randomUUID();
    assertThat(CollectionValidation.tryParseUuid(valid.toString())).contains(valid);
  }

  @Test
  @DisplayName("CreateCollectionRequest serializes and deserializes correctly")
  void createCollectionRequest_serialization() throws Exception {
    var createJson =
        """
        {
          "ownerId": "019273c5-9271-7000-8000-000000000001",
          "name": "Featured Cast",
          "summary": "Primary cast"
        }
        """;
    var createReq = objectMapper.readValue(createJson, CreateCollectionRequest.class);
    assertThat(createReq.ownerId())
        .isEqualTo(UUID.fromString("019273c5-9271-7000-8000-000000000001"));
    assertThat(createReq.name()).isEqualTo("Featured Cast");
    assertThat(createReq.summary()).contains("Primary cast");
  }

  @Test
  @DisplayName("OrderedCollectionResponse serializes compressed and expanded formats")
  void orderedCollectionResponse_serialization() throws Exception {
    var id = UUID.randomUUID();
    var ownerId = UUID.randomUUID();
    var now = Instant.now();
    var itemId = UUID.randomUUID();
    var compressedResponse =
        new OrderedCollectionResponse(
            id,
            "OrderedCollection",
            "Featured Cast",
            Optional.of("Primary cast"),
            ownerId,
            1,
            ImmutableList.of(itemId),
            now,
            now);
    var compressedJson = objectMapper.writeValueAsString(compressedResponse);
    assertThat(compressedJson).contains(id.toString());
    assertThat(compressedJson).contains("Featured Cast");
    assertThat(compressedJson).contains(itemId.toString());
    assertThat(compressedJson).doesNotContain("tenantId");

    var expandedEntity = new ExpandedEntity(itemId, "Individual", "Alice", "Lead actress");
    var expandedResponse =
        new OrderedCollectionResponse(
            id,
            "OrderedCollection",
            "Featured Cast",
            Optional.of("Primary cast"),
            ownerId,
            1,
            ImmutableList.of(expandedEntity),
            now,
            now);
    var expandedJson = objectMapper.writeValueAsString(expandedResponse);
    assertThat(expandedJson).contains("Alice");
    assertThat(expandedJson).contains("Individual");
  }

  @Test
  @DisplayName("AddItemRequest and RemoveItemRequest serialize and deserialize correctly")
  void itemRequests_serialization() throws Exception {
    var addItemJson =
        """
        {
          "refersToId": "019273c5-9271-7000-8000-000000000002",
          "index": 1
        }
        """;
    var addItemReq = objectMapper.readValue(addItemJson, AddItemRequest.class);
    assertThat(addItemReq.refersToId())
        .isEqualTo(UUID.fromString("019273c5-9271-7000-8000-000000000002"));
    assertThat(addItemReq.index()).contains(1);
    assertThat(addItemReq.before()).isEmpty();

    var removeItemJson =
        """
        {
          "refersToId": "019273c5-9271-7000-8000-000000000002"
        }
        """;
    var removeItemReq = objectMapper.readValue(removeItemJson, RemoveItemRequest.class);
    assertThat(removeItemReq.refersToId())
        .isEqualTo(UUID.fromString("019273c5-9271-7000-8000-000000000002"));
  }
}
