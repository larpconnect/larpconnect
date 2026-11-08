package com.larpconnect.njall.api.studios.common;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.api.studios.collections.OrderedCollectionResponse;
import com.larpconnect.njall.data.domain.Collection;
import com.larpconnect.njall.data.domain.ExpandedEntity;

/** Helper for mapping {@link Collection} domain objects to {@link OrderedCollectionResponse}. */
public final class CollectionResponseHelper {

  public static final String TYPE_ORDERED_COLLECTION = "OrderedCollection";

  private CollectionResponseHelper() {}

  public static OrderedCollectionResponse toCompressedResponse(
      String studioIdentifier, Collection collection) {
    return new OrderedCollectionResponse(
        collection.id(),
        TYPE_ORDERED_COLLECTION,
        collection.name(),
        collection.summary(),
        GeneratorUriSupport.formatGeneratorUri(studioIdentifier, collection.owner()),
        collection.ownerId(),
        collection.totalItems(),
        collection.itemIds(),
        collection.createdOn(),
        collection.updatedOn());
  }

  public static OrderedCollectionResponse toExpandedResponse(
      String studioIdentifier, Collection collection, ImmutableList<ExpandedEntity> expandedItems) {
    var items =
        expandedItems.stream()
            .map(
                item ->
                    new ExpandedItemResponse(
                        item.id(),
                        item.type(),
                        GeneratorUriSupport.formatGeneratorUri(studioIdentifier, item.owner()),
                        item.name(),
                        item.summary()))
            .collect(ImmutableList.toImmutableList());
    return new OrderedCollectionResponse(
        collection.id(),
        TYPE_ORDERED_COLLECTION,
        collection.name(),
        collection.summary(),
        GeneratorUriSupport.formatGeneratorUri(studioIdentifier, collection.owner()),
        collection.ownerId(),
        items.size(),
        items,
        collection.createdOn(),
        collection.updatedOn());
  }
}
