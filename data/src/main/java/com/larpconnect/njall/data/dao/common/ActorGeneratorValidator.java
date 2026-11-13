package com.larpconnect.njall.data.dao.common;

import java.util.UUID;
import org.hibernate.Session;
import org.jspecify.annotations.Nullable;

/**
 * Shared validator verifying that an entity generator references an active Actor within the same
 * tenant.
 */
public final class ActorGeneratorValidator {

  private static final String ACTOR_ENTITY_TYPE = "Actor";

  private ActorGeneratorValidator() {}

  /**
   * Checks whether {@code actorId} references an existing, active Actor entity in {@code tenantId}.
   *
   * @param session The active Hibernate session.
   * @param tenantId The tenant UUID.
   * @param actorId The actor entity UUID to check.
   * @return True if the entity exists, is not soft-deleted, and has type {@code Actor}.
   */
  public static boolean isActiveActor(Session session, UUID tenantId, UUID actorId) {
    var entityId = createEntityId(tenantId, actorId);
    var actorEntity = findEntity(session, entityId);
    return isMatchingActiveActor(actorEntity);
  }

  /**
   * Validates that {@code generatorId} references an existing, active Actor entity in {@code
   * tenantId}.
   *
   * @param session The active Hibernate session.
   * @param tenantId The tenant UUID.
   * @param generatorId The generating actor entity UUID to validate.
   * @throws IllegalArgumentException if the generator entity does not exist, is soft-deleted, or is
   *     not an Actor.
   */
  public static void validateActorGenerator(Session session, UUID tenantId, UUID generatorId) {
    var entityId = createEntityId(tenantId, generatorId);
    var generatorEntity = findEntity(session, entityId);
    requireActiveActor(tenantId, generatorId, generatorEntity);
  }

  /**
   * Validates that {@code entityId} references an existing, active entity in {@code tenantId}.
   *
   * @param session The active Hibernate session.
   * @param tenantId The tenant UUID.
   * @param entityId The entity UUID to validate.
   * @throws IllegalArgumentException if the entity does not exist or is soft-deleted.
   */
  public static void validateActiveEntity(Session session, UUID tenantId, UUID entityId) {
    var id = createEntityId(tenantId, entityId);
    var entity = findEntity(session, id);
    requireActiveEntity(tenantId, entityId, entity);
  }

  private static EntityId createEntityId(UUID tenantId, UUID id) {
    return new EntityId(tenantId, id);
  }

  private static @Nullable EntityBaseEntity findEntity(Session session, EntityId entityId) {
    return session.find(EntityBaseEntity.class, entityId);
  }

  private static boolean isMatchingActiveActor(@Nullable EntityBaseEntity actorEntity) {
    return actorEntity != null
        && actorEntity.getDeletedOn() == null
        && ACTOR_ENTITY_TYPE.equals(actorEntity.getEntityType());
  }

  private static void requireActiveActor(
      UUID tenantId, UUID generatorId, @Nullable EntityBaseEntity generatorEntity) {
    if (generatorEntity == null || generatorEntity.getDeletedOn() != null) {
      throw createIllegalArgumentException(
          "Generator entity does not exist or has been deleted in tenant "
              + tenantId
              + ": "
              + generatorId);
    }
    if (!ACTOR_ENTITY_TYPE.equals(generatorEntity.getEntityType())) {
      throw createIllegalArgumentException(
          "Generator entity "
              + generatorId
              + " in tenant "
              + tenantId
              + " is of type '"
              + generatorEntity.getEntityType()
              + "', but must be 'Actor'");
    }
  }

  private static void requireActiveEntity(
      UUID tenantId, UUID entityId, @Nullable EntityBaseEntity entity) {
    if (entity == null || entity.getDeletedOn() != null) {
      throw createIllegalArgumentException(
          "Referenced entity does not exist or has been deleted in tenant "
              + tenantId
              + ": "
              + entityId);
    }
  }

  private static IllegalArgumentException createIllegalArgumentException(String message) {
    return new IllegalArgumentException(message);
  }
}
