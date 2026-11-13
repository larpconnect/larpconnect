package com.larpconnect.njall.data.dao.studios.actors;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;

/** Package-private helper managing collection provisioning and queries for agents. */
final class AgentCollectionsHelper {

  private static final String ENTITY_TYPE_COLLECTION = "OrderedCollection";

  private static final String INSERT_BASE_COLLECTION_SQL =
      """
      INSERT INTO njall_users.entities
        (id, tenant_id, generator, entity_type, summary, created_on, updated_on)
      VALUES (:id, :tenantId, :generator, :entityType, NULL, :now, :now)
      """;

  private static final String INSERT_SUBTYPE_COLLECTION_SQL =
      """
      INSERT INTO njall_users.collections (tenant_id, id, owner_id, name)
      VALUES (:tenantId, :id, :actorId, :name)
      """;

  private static final String QUERY_COLLECTION_ID_SQL =
      """
      SELECT id FROM njall_users.collections
      WHERE tenant_id = :tenantId AND owner_id = :actorId AND name = :name
      """;

  private static final String CASCADE_DELETE_COLLECTIONS_SQL =
      """
      UPDATE njall_users.entities SET deleted_on = :now, updated_on = :now
      WHERE tenant_id = :tenantId AND id IN (
        SELECT id FROM njall_users.collections
        WHERE tenant_id = :tenantId AND owner_id = :actorId)
      """;

  private AgentCollectionsHelper() {}

  static void provisionCollection(
      Session session, UUID tenantId, UUID actorId, UUID id, String name, Instant now) {
    session
        .createNativeQuery(INSERT_BASE_COLLECTION_SQL, Object.class)
        .setParameter("id", id)
        .setParameter("tenantId", tenantId)
        .setParameter("generator", actorId)
        .setParameter("entityType", ENTITY_TYPE_COLLECTION)
        .setParameter("now", now)
        .executeUpdate();

    session
        .createNativeQuery(INSERT_SUBTYPE_COLLECTION_SQL, Object.class)
        .setParameter("tenantId", tenantId)
        .setParameter("id", id)
        .setParameter("actorId", actorId)
        .setParameter("name", name)
        .executeUpdate();
  }

  static void cascadeDeleteCollections(Session session, UUID tenantId, UUID actorId, Instant now) {
    session
        .createNativeQuery(CASCADE_DELETE_COLLECTIONS_SQL, Object.class)
        .setParameter("now", now)
        .setParameter("tenantId", tenantId)
        .setParameter("actorId", actorId)
        .executeUpdate();
  }

  static Optional<UUID> queryCollectionId(
      Session session, UUID tenantId, UUID actorId, String name) {
    List<UUID> rows =
        session
            .createNativeQuery(QUERY_COLLECTION_ID_SQL, UUID.class)
            .setParameter("tenantId", tenantId)
            .setParameter("actorId", actorId)
            .setParameter("name", name)
            .getResultList();
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
  }
}
