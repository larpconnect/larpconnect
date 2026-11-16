package com.larpconnect.njall.data.dao.studios.inbox;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.Immutable;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.larpconnect.njall.data.annotation.NjallUsers;
import com.larpconnect.njall.data.domain.inbox.InboxActivityRecord;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

final class DefaultInboxActivityDAO implements InboxActivityDAO {

  static final int BATCH_CHUNK_SIZE = 500;
  private static final Joiner VALUES_JOINER = Joiner.on(",\n");
  private static final Comparator<InboxActivityRecord> CANONICAL_RECORD_ORDER =
      Comparator.comparing((InboxActivityRecord r) -> r.targetActorId().toString())
          .thenComparing(InboxActivityRecord::activityId);

  private static final String SET_TENANT_CONFIG =
      "SELECT set_config('app.tenant_id', :tenantId, true)";

  private static final String RECURSIVE_TRAVERSAL_PREFIX =
      """
      WITH RECURSIVE ancestor_walk AS (
          SELECT a.id AS origin_id, lower(a.preferred_username) AS origin_username,
                 a.id AS current_id, e.generator AS generator,
                 0 AS depth, ARRAY[a.id] AS visited,
                 (a.id = :receivingActorId) AS reached_receiving
          FROM njall_users.actors a
          JOIN njall_users.entities e ON e.tenant_id = a.tenant_id AND e.id = a.id
          WHERE a.tenant_id = :tenantId
            AND e.deleted_on IS NULL
            AND\s""";

  private static final String RECURSIVE_TRAVERSAL_SUFFIX =
      """

        UNION ALL
          SELECT ca.origin_id, ca.origin_username, e.id AS current_id, e.generator AS generator,
                 ca.depth + 1 AS depth, ca.visited || e.id AS visited,
                 (ca.reached_receiving OR e.id = :receivingActorId) AS reached_receiving
          FROM ancestor_walk ca
          JOIN njall_users.entities e ON e.tenant_id = :tenantId AND e.id = ca.generator
          WHERE e.deleted_on IS NULL
            AND ca.generator != ca.current_id
            AND ca.depth < 64
            AND NOT (e.id = ANY(ca.visited))
      )
      SELECT DISTINCT origin_id, origin_username FROM ancestor_walk
      WHERE reached_receiving AND current_id = generator
      """;

  static final String QUERY_SEEN_BATCH_SQL =
      """
      SELECT activity_id, target_actor_id FROM njall_users.inbox_activities
      WHERE tenant_id = :tenantId
        AND activity_id IN (:activityIds)
        AND target_actor_id IN (:targetActorIds)
        AND expires_on > :now
      """;

  static final String PRUNE_EXPIRED_SQL =
      """
      DELETE FROM njall_users.inbox_activities
      WHERE tenant_id = :tenantId
        AND target_actor_id IN (:targetActorIds)
        AND expires_on <= :receivedOn
      """;

  private static final String RECORD_ACTIVITY_PREFIX =
      """
      INSERT INTO njall_users.inbox_activities (
          tenant_id, target_actor_id, activity_id, received_on, expires_on
      ) VALUES
      """;

  private static final String RECORD_ACTIVITY_ON_CONFLICT =
      """

      ON CONFLICT (tenant_id, target_actor_id, activity_id)
      DO UPDATE SET received_on = EXCLUDED.received_on, expires_on = EXCLUDED.expires_on
      """;

  @Immutable
  private record DeduplicationKey(UUID targetActorId, String activityId) {}

  @Immutable
  private record ParsedCandidates(
      ImmutableList<UUID> uuids, ImmutableList<String> lowerUsernames) {}

  private final Provider<SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultInboxActivityDAO(@NjallUsers Provider<SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  static String buildRecursiveTraversalSql(boolean includeCandidateUuids) {
    var candidatePredicate =
        includeCandidateUuids
            ? "(a.id IN (:candidateUuids) OR lower(a.preferred_username) IN (:candidateUsernames))"
            : "lower(a.preferred_username) IN (:candidateUsernames)";
    return RECURSIVE_TRAVERSAL_PREFIX + candidatePredicate + RECURSIVE_TRAVERSAL_SUFFIX;
  }

  static String buildBatchUpsertSql(int recordCount) {
    var valuesClause =
        VALUES_JOINER.join(
            IntStream.range(0, recordCount)
                .mapToObj(
                    i ->
                        "    (:tenantId, :targetActorId"
                            + i
                            + ", :activityId"
                            + i
                            + ", :receivedOn"
                            + i
                            + ", :expiresOn"
                            + i
                            + ")")
                .iterator());
    return RECORD_ACTIVITY_PREFIX + valuesClause + RECORD_ACTIVITY_ON_CONFLICT;
  }

  @Override
  public ImmutableSet<UUID> resolveRoutableRecipients(
      UUID tenantId, UUID receivingActorId, Set<String> candidateIdentifiers) {
    return ImmutableSet.copyOf(
        resolveRoutableRecipientMap(tenantId, receivingActorId, candidateIdentifiers).values());
  }

  @Override
  public ImmutableMap<String, UUID> resolveRoutableRecipientMap(
      UUID tenantId, UUID receivingActorId, Set<String> candidateIdentifiers) {
    if (candidateIdentifiers.isEmpty()) {
      return ImmutableMap.of();
    }
    var parsed = parseCandidates(candidateIdentifiers);
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var rows = queryRoutableRecipientRows(session, tenantId, receivingActorId, parsed);
        tx.commit();
        return buildRecipientMap(rows);
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  private static ParsedCandidates parseCandidates(Set<String> candidateIdentifiers) {
    var uuidBuilder = ImmutableList.<UUID>builder();
    var usernameBuilder = ImmutableList.<String>builder();
    for (var identifier : candidateIdentifiers) {
      try {
        uuidBuilder.add(UUID.fromString(identifier));
      } catch (IllegalArgumentException ignored) {
        // Not a UUID string
      }
      usernameBuilder.add(identifier.toLowerCase(Locale.ROOT));
    }
    return createParsedCandidates(uuidBuilder.build(), usernameBuilder.build());
  }

  private List<Object[]> queryRoutableRecipientRows(
      Session session, UUID tenantId, UUID receivingActorId, ParsedCandidates parsed) {
    boolean hasUuids = !parsed.uuids().isEmpty();
    var query = session.createNativeQuery(buildRecursiveTraversalSql(hasUuids), Object[].class);
    query.setParameter("tenantId", tenantId);
    query.setParameter("receivingActorId", receivingActorId);
    query.setParameter("candidateUsernames", parsed.lowerUsernames());
    if (hasUuids) {
      query.setParameter("candidateUuids", parsed.uuids());
    }
    return query.getResultList();
  }

  private static ImmutableMap<String, UUID> buildRecipientMap(List<Object[]> rows) {
    var entries = ImmutableList.<java.util.Map.Entry<String, UUID>>builder();
    for (var row : rows) {
      var actorId = (UUID) row[0];
      var lowerUsername = ((String) row[1]).toLowerCase(Locale.ROOT);
      entries.add(java.util.Map.entry(actorId.toString().toLowerCase(Locale.ROOT), actorId));
      entries.add(java.util.Map.entry(lowerUsername, actorId));
    }
    return entries.build().stream()
        .collect(
            ImmutableMap.toImmutableMap(
                java.util.Map.Entry::getKey,
                java.util.Map.Entry::getValue,
                (existing, _) -> existing));
  }

  @Override
  public ImmutableSet<UUID> filterUnseenTargets(
      UUID tenantId, String activityId, Set<UUID> targetActorIds, Instant now) {
    if (targetActorIds.isEmpty()) {
      return ImmutableSet.of();
    }
    var singleBatch =
        ImmutableSetMultimap.<String, UUID>builder().putAll(activityId, targetActorIds).build();
    return filterUnseenTargetsBatch(tenantId, singleBatch, now).get(activityId);
  }

  @Override
  public ImmutableSetMultimap<String, UUID> filterUnseenTargetsBatch(
      UUID tenantId, ImmutableSetMultimap<String, UUID> activityTargets, Instant now) {
    if (activityTargets.isEmpty()) {
      return ImmutableSetMultimap.of();
    }
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var seenPairs =
            querySeenPairs(
                session,
                tenantId,
                ImmutableList.copyOf(activityTargets.keySet()),
                ImmutableList.copyOf(ImmutableSet.copyOf(activityTargets.values())),
                now);
        tx.commit();
        return filterOutSeenPairs(activityTargets, seenPairs);
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  private ImmutableSetMultimap<String, UUID> querySeenPairs(
      Session session,
      UUID tenantId,
      ImmutableList<String> activityIds,
      ImmutableList<UUID> targetActorIds,
      Instant now) {
    var seenBuilder = ImmutableSetMultimap.<String, UUID>builder();
    for (var activityChunk : Lists.partition(activityIds, BATCH_CHUNK_SIZE)) {
      for (var targetChunk : Lists.partition(targetActorIds, BATCH_CHUNK_SIZE)) {
        List<Object[]> rows =
            session
                .createNativeQuery(QUERY_SEEN_BATCH_SQL, Object[].class)
                .setParameter("tenantId", tenantId)
                .setParameter("activityIds", activityChunk)
                .setParameter("targetActorIds", targetChunk)
                .setParameter("now", now)
                .getResultList();
        for (var row : rows) {
          seenBuilder.put((String) row[0], (UUID) row[1]);
        }
      }
    }
    return seenBuilder.build();
  }

  private static ImmutableSetMultimap<String, UUID> filterOutSeenPairs(
      ImmutableSetMultimap<String, UUID> activityTargets,
      ImmutableSetMultimap<String, UUID> seenPairs) {
    var unseenBuilder = ImmutableSetMultimap.<String, UUID>builder();
    for (var entry : activityTargets.entries()) {
      if (!seenPairs.containsEntry(entry.getKey(), entry.getValue())) {
        unseenBuilder.put(entry);
      }
    }
    return unseenBuilder.build();
  }

  @Override
  public void recordAcceptedActivity(
      UUID tenantId,
      String activityId,
      Set<UUID> targetActorIds,
      Instant receivedOn,
      Instant expiresOn) {
    if (targetActorIds.isEmpty()) {
      return;
    }
    var records =
        targetActorIds.stream()
            .map(
                targetActorId ->
                    createActivityRecord(
                        tenantId, targetActorId, activityId, receivedOn, expiresOn))
            .collect(ImmutableList.toImmutableList());
    recordAcceptedActivities(tenantId, records);
  }

  @Override
  public void recordAcceptedActivities(UUID tenantId, ImmutableList<InboxActivityRecord> records) {
    if (records.isEmpty()) {
      return;
    }
    var canonicalRecords = canonicalizeRecords(tenantId, records);
    var orderedDistinctTargets = extractOrderedTargets(canonicalRecords);
    var earliestReceivedOn = findEarliestReceivedOn(canonicalRecords);

    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        for (var targetChunk : Lists.partition(orderedDistinctTargets, BATCH_CHUNK_SIZE)) {
          pruneExpiredActivities(session, tenantId, targetChunk, earliestReceivedOn);
        }
        for (var recordChunk : Lists.partition(canonicalRecords, BATCH_CHUNK_SIZE)) {
          executeBatchUpsert(session, tenantId, recordChunk);
        }
        tx.commit();
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  private static ImmutableList<InboxActivityRecord> canonicalizeRecords(
      UUID tenantId, ImmutableList<InboxActivityRecord> records) {
    for (var record : records) {
      if (!record.tenantId().equals(tenantId)) {
        throw createCrossTenantException(tenantId, record.tenantId());
      }
    }
    var deduplicated =
        records.stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    record -> createDeduplicationKey(record.targetActorId(), record.activityId()),
                    record -> record,
                    (first, second) ->
                        second.expiresOn().isAfter(first.expiresOn()) ? second : first));
    return ImmutableList.sortedCopyOf(CANONICAL_RECORD_ORDER, deduplicated.values());
  }

  private static ImmutableList<UUID> extractOrderedTargets(
      ImmutableList<InboxActivityRecord> canonicalRecords) {
    return canonicalRecords.stream()
        .map(InboxActivityRecord::targetActorId)
        .collect(ImmutableSet.toImmutableSet())
        .asList();
  }

  private static Instant findEarliestReceivedOn(
      ImmutableList<InboxActivityRecord> canonicalRecords) {
    var earliest = canonicalRecords.getFirst().receivedOn();
    for (var record : canonicalRecords) {
      if (record.receivedOn().isBefore(earliest)) {
        earliest = record.receivedOn();
      }
    }
    return earliest;
  }

  private void pruneExpiredActivities(
      Session session, UUID tenantId, List<UUID> targetActorIds, Instant receivedOn) {
    session
        .createNativeQuery(PRUNE_EXPIRED_SQL, Object.class)
        .setParameter("tenantId", tenantId)
        .setParameter("targetActorIds", targetActorIds)
        .setParameter("receivedOn", receivedOn)
        .executeUpdate();
  }

  private void executeBatchUpsert(
      Session session, UUID tenantId, List<InboxActivityRecord> orderedRecords) {
    var upsertQuery =
        session
            .createNativeQuery(buildBatchUpsertSql(orderedRecords.size()), Object.class)
            .setParameter("tenantId", tenantId);
    for (int i = 0; i < orderedRecords.size(); i++) {
      var record = orderedRecords.get(i);
      upsertQuery
          .setParameter("targetActorId" + i, record.targetActorId())
          .setParameter("activityId" + i, record.activityId())
          .setParameter("receivedOn" + i, record.receivedOn())
          .setParameter("expiresOn" + i, record.expiresOn());
    }
    upsertQuery.executeUpdate();
  }

  private void setTenantContext(Session session, UUID tenantId) {
    session
        .createNativeQuery(SET_TENANT_CONFIG, String.class)
        .setParameter("tenantId", tenantId.toString())
        .getSingleResult();
  }

  private static void rollbackQuietly(Transaction tx, Exception e) {
    try {
      tx.rollback();
    } catch (Exception rollbackException) {
      e.addSuppressed(rollbackException);
    }
  }

  private static ParsedCandidates createParsedCandidates(
      ImmutableList<UUID> uuids, ImmutableList<String> lowerUsernames) {
    return new ParsedCandidates(uuids, lowerUsernames);
  }

  private static DeduplicationKey createDeduplicationKey(UUID targetActorId, String activityId) {
    return new DeduplicationKey(targetActorId, activityId);
  }

  private static InboxActivityRecord createActivityRecord(
      UUID tenantId, UUID targetActorId, String activityId, Instant receivedOn, Instant expiresOn) {
    return new InboxActivityRecord(tenantId, targetActorId, activityId, receivedOn, expiresOn);
  }

  private static IllegalArgumentException createCrossTenantException(
      UUID expectedTenantId, UUID actualTenantId) {
    return new IllegalArgumentException(
        "Record tenantId " + actualTenantId + " does not match batch tenantId " + expectedTenantId);
  }
}
