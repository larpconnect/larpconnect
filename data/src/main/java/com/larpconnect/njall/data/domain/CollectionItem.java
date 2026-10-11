package com.larpconnect.njall.data.domain;

import com.google.errorprone.annotations.Immutable;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable domain representation of an item entry within a collection.
 *
 * @param id Unique item identifier.
 * @param collectionId Parent collection UUID.
 * @param ordinal Integer position index within the collection.
 * @param addedOn Timestamp when item was added.
 * @param refersToTenantId Tenant UUID of the referenced entity.
 * @param refersToId UUID of the referenced entity.
 */
@Immutable
public record CollectionItem(
    UUID id,
    UUID collectionId,
    int ordinal,
    Instant addedOn,
    UUID refersToTenantId,
    UUID refersToId)
    implements DatabaseObject {}
