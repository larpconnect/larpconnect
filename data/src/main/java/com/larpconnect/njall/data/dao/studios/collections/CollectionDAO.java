package com.larpconnect.njall.data.dao.studios.collections;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.domain.Collection;
import com.larpconnect.njall.data.domain.ExpandedEntity;
import java.util.Optional;
import java.util.UUID;

/** DAO interface for tenanted studio collections and collection items. */
public interface CollectionDAO {

  /**
   * Retrieves a collection by tenant and ID, including ordered active item IDs.
   *
   * @param tenantId Studio tenant UUID.
   * @param collectionId Collection UUID.
   * @return Optional containing the collection if found and active.
   */
  Optional<Collection> findById(UUID tenantId, UUID collectionId);

  /**
   * Retrieves active items in a collection expanded as entities with names/summaries.
   *
   * @param tenantId Studio tenant UUID.
   * @param collectionId Collection UUID.
   * @return Ordered list of expanded entities.
   */
  ImmutableList<ExpandedEntity> findExpandedItems(UUID tenantId, UUID collectionId);

  /**
   * Checks if an active collection exists for the given owner and name.
   *
   * @param tenantId Studio tenant UUID.
   * @param ownerId Owner entity UUID.
   * @param name Collection name.
   * @param excludeId Optional collection ID to exclude (for PATCH).
   * @return True if another collection already exists with that owner and name.
   */
  boolean existsByOwnerAndName(UUID tenantId, UUID ownerId, String name, Optional<UUID> excludeId);

  /**
   * Creates a new collection under the specified studio tenant.
   *
   * @param tenantId Studio tenant UUID.
   * @param owner Generating actor entity UUID.
   * @param ownerId Semantic owner entity UUID.
   * @param name Collection name.
   * @param summary Optional description summary.
   * @return Created collection.
   */
  Collection create(UUID tenantId, UUID owner, UUID ownerId, String name, Optional<String> summary);

  /**
   * Applies partial updates to a collection.
   *
   * @param tenantId Studio tenant UUID.
   * @param collectionId Collection UUID.
   * @param name Optional new collection name.
   * @param summary Optional new summary.
   * @return Optional containing the updated collection if found and active.
   */
  Optional<Collection> patch(
      UUID tenantId, UUID collectionId, Optional<String> name, Optional<String> summary);

  /**
   * Soft-deletes a collection.
   *
   * @param tenantId Studio tenant UUID.
   * @param collectionId Collection UUID.
   * @return True if collection was active and soft-deleted, false otherwise.
   */
  boolean softDelete(UUID tenantId, UUID collectionId);

  /**
   * Adds an entity reference to a collection, optionally before another item or at an index.
   *
   * @param tenantId Studio tenant UUID.
   * @param collectionId Collection UUID.
   * @param refersToId Referenced entity UUID.
   * @param before Optional entity UUID before which to insert.
   * @param index Optional zero-based index for insertion.
   * @return True if successfully added or already present (idempotent), false if collection not
   *     found.
   */
  boolean addItem(
      UUID tenantId,
      UUID collectionId,
      UUID refersToId,
      Optional<UUID> before,
      Optional<Integer> index);

  /**
   * Removes an entity reference from a collection.
   *
   * @param tenantId Studio tenant UUID.
   * @param collectionId Collection UUID.
   * @param refersToId Referenced entity UUID to remove.
   * @return True if successfully removed or idempotent, false if collection not found.
   */
  boolean removeItem(UUID tenantId, UUID collectionId, UUID refersToId);
}
