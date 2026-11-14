package com.larpconnect.njall.api.studios.common;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.api.studios.collections.OrderedCollectionResponse;
import com.larpconnect.njall.data.domain.studios.Collection;
import com.larpconnect.njall.data.domain.studios.ExpandedEntity;

/** Helper for mapping {@link Collection} domain objects to {@link OrderedCollectionResponse}. */
@Immutable
public final class CollectionResponseHelper {

  public static final String TYPE_ORDERED_COLLECTION = "OrderedCollection";

  private CollectionResponseHelper() {}

  public static OrderedCollectionResponse toCompressedResponse(
      String studioIdentifier, Collection collection) {
    var generatorUri =
        GeneratorUriSupport.formatGeneratorUri(studioIdentifier, collection.generator());
    return createOrderedResponse(
        collection, generatorUri, collection.totalItems(), collection.itemIds());
  }

  public static OrderedCollectionResponse toExpandedResponse(
      String studioIdentifier, Collection collection, ImmutableList<ExpandedEntity> expandedItems) {
    var items =
        expandedItems.stream()
            .map(item -> toExpandedItemResponse(studioIdentifier, item))
            .collect(ImmutableList.toImmutableList());
    var generatorUri =
        GeneratorUriSupport.formatGeneratorUri(studioIdentifier, collection.generator());
    return createOrderedResponse(collection, generatorUri, items.size(), items);
  }

  private static ExpandedItemResponse toExpandedItemResponse(
      String studioIdentifier, ExpandedEntity item) {
    var generatorUri = GeneratorUriSupport.formatGeneratorUri(studioIdentifier, item.generator());
    return createExpandedItemResponse(item, generatorUri);
  }

  private static ExpandedItemResponse createExpandedItemResponse(
      ExpandedEntity item, String generatorUri) {
    return new ExpandedItemResponse(
        item.id(), item.type(), generatorUri, item.name(), item.summary());
  }

  private static OrderedCollectionResponse createOrderedResponse(
      Collection collection, String generatorUri, int totalItems, ImmutableList<?> orderedItems) {
    return new OrderedCollectionResponse(
        collection.id(),
        TYPE_ORDERED_COLLECTION,
        collection.name(),
        collection.summary(),
        generatorUri,
        collection.ownerId(),
        totalItems,
        orderedItems,
        collection.createdOn(),
        collection.updatedOn());
  }
}
