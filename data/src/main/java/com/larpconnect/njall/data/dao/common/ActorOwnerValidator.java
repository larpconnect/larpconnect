package com.larpconnect.njall.data.dao.common;

import java.util.UUID;
import org.hibernate.Session;

/**
 * Shared validator verifying that an entity owner references an active Actor within the same
 * tenant.
 */
public final class ActorOwnerValidator {

  private static final String ACTOR_ENTITY_TYPE = "Actor";

  private ActorOwnerValidator() {}

  /**
   * Validates that {@code ownerId} references an existing, active Actor entity in {@code tenantId}.
   *
   * @param session The active Hibernate session.
   * @param tenantId The tenant UUID.
   * @param ownerId The owner entity UUID to validate.
   * @throws IllegalArgumentException if the owner entity does not exist, is soft-deleted, or is not
   *     an Actor.
   */
  public static void validateActorOwner(Session session, UUID tenantId, UUID ownerId) {
    var ownerEntity = session.find(EntityBaseEntity.class, createEntityId(tenantId, ownerId));
    if (ownerEntity == null || ownerEntity.getDeletedOn() != null) {
      throw new IllegalArgumentException(
          "Owner entity does not exist or has been deleted in tenant " + tenantId + ": " + ownerId);
    }
    if (!ACTOR_ENTITY_TYPE.equals(ownerEntity.getEntityType())) {
      throw new IllegalArgumentException(
          "Owner entity "
              + ownerId
              + " in tenant "
              + tenantId
              + " is of type '"
              + ownerEntity.getEntityType()
              + "', but must be 'Actor'");
    }
  }

  private static EntityId createEntityId(UUID tenantId, UUID id) {
    return new EntityId(tenantId, id);
  }
}
