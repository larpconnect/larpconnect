package com.larpconnect.njall.data.dao.studios.inbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.larpconnect.njall.data.domain.inbox.InboxActivityRecord;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class InboxActivityDAOTest {

  private SessionFactory sessionFactory;
  private Session session;
  private Transaction tx;
  private DefaultInboxActivityDAO dao;

  private final UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000010");
  private final UUID receivingActorId = UUID.fromString("00000000-0000-0000-0000-000000000020");
  private final UUID targetActorId1 = UUID.fromString("00000000-0000-0000-0000-000000000101");
  private final UUID targetActorId2 = UUID.fromString("00000000-0000-0000-0000-000000000202");
  private final Instant now = Instant.parse("2026-11-14T12:00:00Z");
  private final Instant expiresOn = now.plusSeconds(8 * 86400);

  @BeforeEach
  void setUp() {
    sessionFactory = mock(SessionFactory.class);
    session = mock(Session.class);
    tx = mock(Transaction.class);
    when(sessionFactory.openSession()).thenReturn(session);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    dao = new DefaultInboxActivityDAO(() -> sessionFactory);
  }

  @Test
  @DisplayName("buildRecursiveTraversalSql deduplicates CTE and toggles candidateUuids predicate")
  void buildRecursiveTraversalSql_togglesUuidPredicate() {
    var withUuids = DefaultInboxActivityDAO.buildRecursiveTraversalSql(true);
    var onlyUsernames = DefaultInboxActivityDAO.buildRecursiveTraversalSql(false);

    assertThat(withUuids)
        .contains("WITH RECURSIVE ancestor_walk")
        .contains("a.id IN (:candidateUuids)")
        .contains("lower(a.preferred_username) IN (:candidateUsernames)")
        .contains("SELECT DISTINCT origin_id, origin_username FROM ancestor_walk")
        .contains("WHERE reached_receiving AND current_id = generator")
        .contains("ca.depth < 64");

    assertThat(onlyUsernames)
        .contains("WITH RECURSIVE ancestor_walk")
        .doesNotContain(":candidateUuids")
        .contains("lower(a.preferred_username) IN (:candidateUsernames)")
        .contains("WHERE reached_receiving AND current_id = generator")
        .contains("ca.depth < 64");
  }

  @Test
  @DisplayName("buildBatchUpsertSql generates multi-row VALUES with ON CONFLICT DO UPDATE")
  void buildBatchUpsertSql_generatesMultiRowValues() {
    var sql = DefaultInboxActivityDAO.buildBatchUpsertSql(2);

    assertThat(sql)
        .contains("INSERT INTO njall_users.inbox_activities")
        .contains("(:tenantId, :targetActorId0, :activityId0, :receivedOn0, :expiresOn0),")
        .contains("(:tenantId, :targetActorId1, :activityId1, :receivedOn1, :expiresOn1)")
        .contains("ON CONFLICT (tenant_id, target_actor_id, activity_id)")
        .contains("DO UPDATE SET received_on = EXCLUDED.received_on");
  }

  @Test
  @DisplayName("resolveRoutableRecipients returns empty when candidates set is empty")
  void resolveRoutableRecipients_emptyCandidates() {
    var result = dao.resolveRoutableRecipients(tenantId, receivingActorId, Set.of());
    assertThat(result).isEmpty();
    assertThat(dao.resolveRoutableRecipientMap(tenantId, receivingActorId, Set.of())).isEmpty();
    verify(sessionFactory, never()).openSession();
  }

  @Test
  @DisplayName(
      "resolveRoutableRecipientMap maps both lowercase UUIDs and lowercase usernames to actor IDs")
  void resolveRoutableRecipientMap_withUuidsAndUsernames_success() {
    NativeQuery<Object[]> query = mock();
    when(session.createNativeQuery(contains("WITH RECURSIVE ancestor_walk"), eq(Object[].class)))
        .thenReturn(query);
    when(query.setParameter(any(String.class), any())).thenReturn(query);
    var rows =
        ImmutableList.<Object[]>of(
            new Object[] {targetActorId1, "valkyrie_lead"},
            new Object[] {targetActorId2, targetActorId2.toString()});
    when(query.getResultList()).thenReturn(rows);

    var candidates = Set.of(targetActorId1.toString(), "VALKYRIE_LEAD");
    var map = dao.resolveRoutableRecipientMap(tenantId, receivingActorId, candidates);

    assertThat(map)
        .containsEntry(targetActorId1.toString().toLowerCase(Locale.ROOT), targetActorId1)
        .containsEntry("valkyrie_lead", targetActorId1)
        .containsEntry(targetActorId2.toString().toLowerCase(Locale.ROOT), targetActorId2);
    verify(query).setParameter("candidateUuids", List.of(targetActorId1));
    verify(tx).commit();
  }

  @Test
  @DisplayName("resolveRoutableRecipients resolves recipients when only usernames provided")
  void resolveRoutableRecipients_withOnlyUsernames_success() {
    NativeQuery<Object[]> query = mock();
    when(session.createNativeQuery(contains("WITH RECURSIVE ancestor_walk"), eq(Object[].class)))
        .thenReturn(query);
    when(query.setParameter(any(String.class), any())).thenReturn(query);
    var rows = ImmutableList.<Object[]>of(new Object[] {targetActorId1, "valkyrie"});
    when(query.getResultList()).thenReturn(rows);

    var candidates = Set.of("valkyrie", "npc_sheriff");
    var result = dao.resolveRoutableRecipients(tenantId, receivingActorId, candidates);

    assertThat(result).containsExactly(targetActorId1);
    verify(tx).commit();
  }

  @Test
  @DisplayName("resolveRoutableRecipients rolls back and rethrows on query error")
  void resolveRoutableRecipients_rollbackOnError() {
    when(session.createNativeQuery(contains("WITH RECURSIVE ancestor_walk"), eq(Object[].class)))
        .thenThrow(new RuntimeException("CTE query error"));
    doThrow(new RuntimeException("Rollback secondary failure")).when(tx).rollback();

    var thrown = new Throwable[1];
    assertThatThrownBy(
            () -> {
              try {
                dao.resolveRoutableRecipients(tenantId, receivingActorId, Set.of("valkyrie"));
              } catch (RuntimeException ex) {
                thrown[0] = ex;
                throw ex;
              }
            })
        .isInstanceOf(RuntimeException.class);
    verify(tx).rollback();
    assertThat(thrown[0].getSuppressed()).hasSize(1);
  }

  @Test
  @DisplayName("filterUnseenTargets and filterUnseenTargetsBatch return empty on empty input")
  void filterUnseenTargets_emptyTargets() {
    assertThat(dao.filterUnseenTargets(tenantId, "act-1", Set.of(), now)).isEmpty();
    assertThat(dao.filterUnseenTargetsBatch(tenantId, ImmutableSetMultimap.of(), now).entries())
        .isEmpty();
    verify(sessionFactory, never()).openSession();
  }

  @Test
  @DisplayName("filterUnseenTargetsBatch filters unseen (activityId, targetActorId) pairs")
  void filterUnseenTargetsBatch_filtersOutSeenPairs() {
    NativeQuery<Object[]> query = mock();
    when(session.createNativeQuery(
            contains("FROM njall_users.inbox_activities"), eq(Object[].class)))
        .thenReturn(query);
    when(query.setParameter(any(String.class), any())).thenReturn(query);
    var seenRows =
        ImmutableList.<Object[]>of(
            new Object[] {"act-1", targetActorId1}, new Object[] {"act-2", targetActorId2});
    when(query.getResultList()).thenReturn(seenRows);

    var input =
        ImmutableSetMultimap.<String, UUID>builder()
            .putAll("act-1", targetActorId1, targetActorId2)
            .putAll("act-2", targetActorId1, targetActorId2)
            .build();
    var unseen = dao.filterUnseenTargetsBatch(tenantId, input, now);

    assertThat(unseen.get("act-1")).containsExactly(targetActorId2);
    assertThat(unseen.get("act-2")).containsExactly(targetActorId1);
    verify(query).setParameter("now", now);
    verify(tx).commit();
  }

  @Test
  @DisplayName("filterUnseenTargets returns empty when all targets are already seen")
  void filterUnseenTargets_allSeen() {
    NativeQuery<Object[]> query = mock();
    when(session.createNativeQuery(
            contains("FROM njall_users.inbox_activities"), eq(Object[].class)))
        .thenReturn(query);
    when(query.setParameter(any(String.class), any())).thenReturn(query);
    var seenRows =
        ImmutableList.<Object[]>of(
            new Object[] {"act-1", targetActorId1}, new Object[] {"act-1", targetActorId2});
    when(query.getResultList()).thenReturn(seenRows);

    var targets = Set.of(targetActorId1, targetActorId2);
    var result = dao.filterUnseenTargets(tenantId, "act-1", targets, now);

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("filterUnseenTargets rolls back and rethrows on query error")
  void filterUnseenTargets_rollbackOnError() {
    when(session.createNativeQuery(
            contains("FROM njall_users.inbox_activities"), eq(Object[].class)))
        .thenThrow(new RuntimeException("Query error"));

    assertThatThrownBy(
            () -> dao.filterUnseenTargets(tenantId, "act-1", Set.of(targetActorId1), now))
        .isInstanceOf(RuntimeException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("recordAcceptedActivity and recordAcceptedActivities no-op on empty input")
  void recordAcceptedActivity_emptyTargets() {
    dao.recordAcceptedActivity(tenantId, "act-1", Set.of(), now, expiresOn);
    dao.recordAcceptedActivities(tenantId, ImmutableList.of());
    verify(sessionFactory, never()).openSession();
  }

  @Test
  @DisplayName(
      "recordAcceptedActivities deduplicates duplicate keys and sorts canonically by"
          + " (targetActorId, activityId)")
  void recordAcceptedActivities_sortsCanonicallyAndDeduplicates() {
    NativeQuery<Object> pruneQuery = mock();
    when(session.createNativeQuery(
            contains("DELETE FROM njall_users.inbox_activities"), eq(Object.class)))
        .thenReturn(pruneQuery);
    when(pruneQuery.setParameter(any(String.class), any())).thenReturn(pruneQuery);
    when(pruneQuery.executeUpdate()).thenReturn(1);

    NativeQuery<Object> upsertQuery = mock();
    when(session.createNativeQuery(
            contains("INSERT INTO njall_users.inbox_activities"), eq(Object.class)))
        .thenReturn(upsertQuery);
    when(upsertQuery.setParameter(any(String.class), any())).thenReturn(upsertQuery);
    when(upsertQuery.executeUpdate()).thenReturn(3);

    var earlierNow = now.minusSeconds(10);
    var laterExpires = expiresOn.plusSeconds(60);
    var records =
        ImmutableList.of(
            new InboxActivityRecord(tenantId, targetActorId2, "act-b", now, expiresOn),
            new InboxActivityRecord(tenantId, targetActorId1, "act-b", now, expiresOn),
            new InboxActivityRecord(tenantId, targetActorId1, "act-a", earlierNow, expiresOn),
            new InboxActivityRecord(tenantId, targetActorId1, "act-a", now, laterExpires),
            new InboxActivityRecord(
                tenantId, targetActorId1, "act-a", now, expiresOn.minusSeconds(30)));

    dao.recordAcceptedActivities(tenantId, records);

    var order = inOrder(pruneQuery, upsertQuery, tx);
    order.verify(pruneQuery).setParameter("tenantId", tenantId);
    order
        .verify(pruneQuery)
        .setParameter("targetActorIds", ImmutableList.of(targetActorId1, targetActorId2));
    order.verify(pruneQuery).setParameter("receivedOn", now);
    order.verify(pruneQuery, times(1)).executeUpdate();

    order.verify(upsertQuery).setParameter("tenantId", tenantId);
    order.verify(upsertQuery).setParameter("targetActorId0", targetActorId1);
    order.verify(upsertQuery).setParameter("activityId0", "act-a");
    order.verify(upsertQuery).setParameter("expiresOn0", laterExpires);
    order.verify(upsertQuery).setParameter("targetActorId1", targetActorId1);
    order.verify(upsertQuery).setParameter("activityId1", "act-b");
    order.verify(upsertQuery).setParameter("targetActorId2", targetActorId2);
    order.verify(upsertQuery).setParameter("activityId2", "act-b");
    order.verify(upsertQuery, times(1)).executeUpdate();
    order.verify(tx).commit();
  }

  @Test
  @DisplayName("recordAcceptedActivities tracks earliest receivedOn across records")
  void recordAcceptedActivities_usesEarliestReceivedOnForPrune() {
    NativeQuery<Object> pruneQuery = mock();
    when(session.createNativeQuery(
            contains("DELETE FROM njall_users.inbox_activities"), eq(Object.class)))
        .thenReturn(pruneQuery);
    when(pruneQuery.setParameter(any(String.class), any())).thenReturn(pruneQuery);
    when(pruneQuery.executeUpdate()).thenReturn(0);

    NativeQuery<Object> upsertQuery = mock();
    when(session.createNativeQuery(
            contains("INSERT INTO njall_users.inbox_activities"), eq(Object.class)))
        .thenReturn(upsertQuery);
    when(upsertQuery.setParameter(any(String.class), any())).thenReturn(upsertQuery);
    when(upsertQuery.executeUpdate()).thenReturn(2);

    var earlier = now.minusSeconds(30);
    var records =
        ImmutableList.of(
            new InboxActivityRecord(tenantId, targetActorId1, "act-a", now, expiresOn),
            new InboxActivityRecord(tenantId, targetActorId2, "act-b", earlier, expiresOn));

    dao.recordAcceptedActivities(tenantId, records);

    verify(pruneQuery).setParameter("receivedOn", earlier);
  }

  @Test
  @DisplayName("recordAcceptedActivities rejects cross-tenant records before opening a session")
  void recordAcceptedActivities_crossTenant_throws() {
    var wrongTenant = UUID.randomUUID();
    var records =
        ImmutableList.of(
            new InboxActivityRecord(wrongTenant, targetActorId1, "act-1", now, expiresOn));

    assertThatThrownBy(() -> dao.recordAcceptedActivities(tenantId, records))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not match batch tenantId");
    verify(sessionFactory, never()).openSession();
  }

  @Test
  @DisplayName(
      "recordAcceptedActivity and filterUnseenTargets partition large target sets into chunks")
  void largeTargetSet_partitionsIntoChunksWithinSingleTransaction() {
    NativeQuery<Object> pruneQuery = mock();
    when(session.createNativeQuery(
            contains("DELETE FROM njall_users.inbox_activities"), eq(Object.class)))
        .thenReturn(pruneQuery);
    when(pruneQuery.setParameter(any(String.class), any())).thenReturn(pruneQuery);
    when(pruneQuery.executeUpdate()).thenReturn(0);

    NativeQuery<Object> upsertQuery = mock();
    when(session.createNativeQuery(
            contains("INSERT INTO njall_users.inbox_activities"), eq(Object.class)))
        .thenReturn(upsertQuery);
    when(upsertQuery.setParameter(any(String.class), any())).thenReturn(upsertQuery);
    when(upsertQuery.executeUpdate()).thenReturn(1);

    NativeQuery<Object[]> seenQuery = mock();
    when(session.createNativeQuery(
            contains("FROM njall_users.inbox_activities"), eq(Object[].class)))
        .thenReturn(seenQuery);
    when(seenQuery.setParameter(any(String.class), any())).thenReturn(seenQuery);
    when(seenQuery.getResultList()).thenReturn(List.of());

    int totalTargets = DefaultInboxActivityDAO.BATCH_CHUNK_SIZE + 5;
    ImmutableSet<UUID> largeTargets =
        IntStream.range(0, totalTargets)
            .mapToObj(_ -> UUID.randomUUID())
            .collect(ImmutableSet.toImmutableSet());

    var unseen = dao.filterUnseenTargets(tenantId, "act-large", largeTargets, now);
    assertThat(unseen).hasSize(totalTargets);
    verify(seenQuery, times(2)).getResultList();

    dao.recordAcceptedActivity(tenantId, "act-large", largeTargets, now, expiresOn);
    verify(pruneQuery, times(2)).executeUpdate();
    verify(upsertQuery, times(2)).executeUpdate();
  }

  @Test
  @DisplayName("recordAcceptedActivity rolls back and rethrows on execution error")
  void recordAcceptedActivity_rollbackOnError() {
    NativeQuery<Object> pruneQuery = mock();
    when(session.createNativeQuery(
            contains("DELETE FROM njall_users.inbox_activities"), eq(Object.class)))
        .thenReturn(pruneQuery);
    when(pruneQuery.setParameter(any(String.class), any())).thenReturn(pruneQuery);
    when(pruneQuery.executeUpdate()).thenReturn(0);

    when(session.createNativeQuery(
            contains("INSERT INTO njall_users.inbox_activities"), eq(Object.class)))
        .thenThrow(new RuntimeException("Insert error"));

    assertThatThrownBy(
            () ->
                dao.recordAcceptedActivity(
                    tenantId, "act-1", Set.of(targetActorId1), now, expiresOn))
        .isInstanceOf(RuntimeException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("InboxActivityEntity and InboxActivityId entity contracts and toDomain behave")
  void entityAndId_contracts() {
    var id1 = new InboxActivityId(tenantId, targetActorId1, "act-1");
    var id2 = new InboxActivityId(tenantId, targetActorId1, "act-1");
    var id3 = new InboxActivityId(tenantId, targetActorId2, "act-2");

    Object sameRef = id1;
    assertThat(id1).isEqualTo(sameRef).isEqualTo(id2).hasSameHashCodeAs(id2);
    assertThat(id1).isNotEqualTo(id3);
    assertThat(id1).isNotEqualTo(new InboxActivityId(tenantId, targetActorId2, "act-1"));
    assertThat(id1).isNotEqualTo(new InboxActivityId(UUID.randomUUID(), targetActorId1, "act-1"));
    assertThat(id1).isNotEqualTo(null);
    assertThat(id1).isNotEqualTo("string");
    assertThat(id1.getTenantId()).isEqualTo(tenantId);
    assertThat(id1.getTargetActorId()).isEqualTo(targetActorId1);
    assertThat(id1.getActivityId()).isEqualTo("act-1");

    var emptyId = new InboxActivityId();
    assertThat(emptyId.getTenantId()).isEqualTo(new UUID(0L, 0L));
    assertThat(emptyId.getTargetActorId()).isEqualTo(new UUID(0L, 0L));
    assertThat(emptyId.getActivityId()).isEmpty();

    var entity = new InboxActivityEntity(id1, now, expiresOn);
    assertThat(entity.getId()).isEqualTo(id1);
    assertThat(entity.getReceivedOn()).isEqualTo(now);
    assertThat(entity.getExpiresOn()).isEqualTo(expiresOn);
    assertThat(entity.toDomain())
        .isEqualTo(new InboxActivityRecord(tenantId, targetActorId1, "act-1", now, expiresOn));

    var emptyEntity = new InboxActivityEntity();
    assertThat(emptyEntity.getId()).isEqualTo(emptyId);
    assertThat(emptyEntity.getReceivedOn()).isEqualTo(Instant.EPOCH);
    emptyEntity.setExpiresOn(expiresOn);
    assertThat(emptyEntity.getExpiresOn()).isEqualTo(expiresOn);
  }

  private void mockTenantConfigQuery() {
    NativeQuery<String> query = mock();
    when(session.createNativeQuery(contains("set_config"), eq(String.class))).thenReturn(query);
    when(query.setParameter(eq("tenantId"), any())).thenReturn(query);
    when(query.getSingleResult()).thenReturn("tenant-set");
  }
}
