package com.larpconnect.njall.data.dao.studios.collections;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.domain.studios.ExpandedEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;

/** Package-private helper executing collection item and expanded entity queries and mutations. */
@Immutable
final class CollectionItemHelper {

  static final String EXPANDED_ITEMS_QUERY =
      """
      SELECT e.id, e.generator, e.entity_type, e.summary,
             COALESCE(
                 i.name,
                 ev.title,
                 loc.name,
                 l.url,
                 a.preferred_username,
                 c.name,
                 sys.name,
                 cmp.title
             ) AS display_name
      FROM njall_users.collection_items ci
      JOIN njall_users.entities e
        ON ci.refers_to_tenant_id = e.tenant_id AND ci.refers_to_id = e.id
      LEFT JOIN njall_users.individuals i
        ON e.tenant_id = i.tenant_id AND e.id = i.id
      LEFT JOIN njall_users.events ev
        ON e.tenant_id = ev.tenant_id AND e.id = ev.id
      LEFT JOIN njall_users.locations loc
        ON e.tenant_id = loc.tenant_id AND e.id = loc.id
      LEFT JOIN njall_users.links l
        ON e.tenant_id = l.tenant_id AND e.id = l.id
      LEFT JOIN njall_users.actors a
        ON e.tenant_id = a.tenant_id AND e.id = a.id
      LEFT JOIN njall_users.collections c
        ON e.tenant_id = c.tenant_id AND e.id = c.id
      LEFT JOIN njall_users.systems sys
        ON e.tenant_id = sys.tenant_id AND e.id = sys.id
      LEFT JOIN njall_users.campaigns cmp
        ON e.tenant_id = cmp.tenant_id AND e.id = cmp.id
      WHERE ci.tenant_id = :tenantId
        AND ci.collection_id = :collectionId
        AND e.deleted_on IS NULL
      ORDER BY ci.ordinal ASC
      """;

  static final String QUERY_ACTIVE_ITEM_IDS_HQL =
      """
      SELECT ci.refersToId FROM CollectionItemEntity ci, EntityBaseEntity e
      WHERE ci.tenantId = :tenantId AND ci.collectionId = :collectionId
        AND ci.refersToTenantId = e.tenantId AND ci.refersToId = e.id
        AND e.deletedOn IS NULL
      ORDER BY ci.ordinal ASC
      """;

  static final String COUNT_ITEM_BY_REFERS_TO_HQL =
      """
      SELECT COUNT(ci) FROM CollectionItemEntity ci
      WHERE ci.tenantId = :tenantId AND ci.collectionId = :collectionId
        AND ci.refersToId = :refersToId
      """;

  static final String FIND_ITEM_BY_REFERS_TO_HQL =
      """
      FROM CollectionItemEntity ci
      WHERE ci.tenantId = :tenantId AND ci.collectionId = :collectionId
        AND ci.refersToId = :refersToId
      """;

  static final String SHIFT_ORDINALS_UP_HQL =
      """
      UPDATE CollectionItemEntity ci SET ci.ordinal = ci.ordinal + 1
      WHERE ci.tenantId = :tenantId AND ci.collectionId = :collectionId
        AND ci.ordinal >= :targetOrdinal
      """;

  static final String SHIFT_ORDINALS_DOWN_HQL =
      """
      UPDATE CollectionItemEntity ci SET ci.ordinal = ci.ordinal - 1
      WHERE ci.tenantId = :tenantId AND ci.collectionId = :collectionId
        AND ci.ordinal > :removedOrdinal
      """;

  private CollectionItemHelper() {}

  static ImmutableList<ExpandedEntity> queryExpandedItems(
      Session session, UUID tenantId, UUID collectionId) {
    List<Object[]> rows =
        session
            .createNativeQuery(EXPANDED_ITEMS_QUERY, Object[].class)
            .setParameter("tenantId", tenantId)
            .setParameter("collectionId", collectionId)
            .getResultList();
    return rows.stream()
        .map(CollectionItemHelper::toExpandedEntity)
        .collect(ImmutableList.toImmutableList());
  }

  private static ExpandedEntity toExpandedEntity(Object[] row) {
    return new ExpandedEntity(
        (UUID) row[0], (UUID) row[1], (String) row[2], (String) row[4], (String) row[3]);
  }

  static ImmutableList<UUID> queryActiveItemIds(Session session, UUID tenantId, UUID collectionId) {
    List<UUID> results =
        session
            .createQuery(QUERY_ACTIVE_ITEM_IDS_HQL, UUID.class)
            .setParameter("tenantId", tenantId)
            .setParameter("collectionId", collectionId)
            .getResultList();
    return ImmutableList.copyOf(results);
  }

  static boolean isItemAlreadyPresent(
      Session session, UUID tenantId, UUID collectionId, UUID refersToId) {
    long count =
        session
            .createQuery(COUNT_ITEM_BY_REFERS_TO_HQL, Long.class)
            .setParameter("tenantId", tenantId)
            .setParameter("collectionId", collectionId)
            .setParameter("refersToId", refersToId)
            .getSingleResult();
    return count > 0;
  }

  static int resolveTargetOrdinal(
      Session session,
      UUID tenantId,
      UUID collectionId,
      Optional<UUID> before,
      Optional<Integer> index) {
    if (before.isPresent()) {
      var beforeItem = findItemByRefersToId(session, tenantId, collectionId, before.orElseThrow());
      return beforeItem.map(CollectionItemEntity::getOrdinal).orElse(0);
    }
    return index.map(i -> Math.max(0, i)).orElse(0);
  }

  static Optional<CollectionItemEntity> findItemByRefersToId(
      Session session, UUID tenantId, UUID collectionId, UUID refersToId) {
    return session
        .createQuery(FIND_ITEM_BY_REFERS_TO_HQL, CollectionItemEntity.class)
        .setParameter("tenantId", tenantId)
        .setParameter("collectionId", collectionId)
        .setParameter("refersToId", refersToId)
        .uniqueResultOptional();
  }

  static void shiftOrdinalsUp(
      Session session, UUID tenantId, UUID collectionId, int targetOrdinal) {
    session
        .createMutationQuery(SHIFT_ORDINALS_UP_HQL)
        .setParameter("tenantId", tenantId)
        .setParameter("collectionId", collectionId)
        .setParameter("targetOrdinal", targetOrdinal)
        .executeUpdate();
  }

  static void shiftOrdinalsDown(
      Session session, UUID tenantId, UUID collectionId, int removedOrdinal) {
    session
        .createMutationQuery(SHIFT_ORDINALS_DOWN_HQL)
        .setParameter("tenantId", tenantId)
        .setParameter("collectionId", collectionId)
        .setParameter("removedOrdinal", removedOrdinal)
        .executeUpdate();
  }

  static CollectionItemEntity createCollectionItemEntity(
      UUID id, UUID tenantId, UUID collectionId, int ordinal, Instant addedOn, UUID refersToId) {
    return new CollectionItemEntity(
        id, tenantId, collectionId, ordinal, addedOn, tenantId, refersToId);
  }
}
