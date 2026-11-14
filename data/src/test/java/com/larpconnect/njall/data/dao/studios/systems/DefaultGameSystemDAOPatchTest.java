package com.larpconnect.njall.data.dao.studios.systems;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.dao.common.EntityBaseEntity;
import com.larpconnect.njall.data.dao.common.EntityId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

final class DefaultGameSystemDAOPatchTest {

  private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID GENERATOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
  private static final UUID SYSTEM_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
  private static final UUID OTHER_SYSTEM_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000004");
  private static final Instant CREATED_ON = Instant.parse("2026-01-10T12:00:00Z");
  private static final Instant UPDATED_ON = Instant.parse("2026-01-11T12:00:00Z");
  private static final String TENANT_SQL = "SELECT set_config('app.tenant_id', :tenantId, true)";

  private final SessionFactory sessionFactory = mock(SessionFactory.class);
  private final Session session = mock(Session.class);
  private final Transaction tx = mock(Transaction.class);
  private final NativeQuery<String> tenantQuery = mockNativeQuery();
  private final DefaultGameSystemDAO dao = createDao(sessionFactory);

  @BeforeEach
  void setUp() {
    when(sessionFactory.openSession()).thenReturn(session);
    when(session.beginTransaction()).thenReturn(tx);
    when(session.createNativeQuery(TENANT_SQL, String.class)).thenReturn(tenantQuery);
    when(tenantQuery.setParameter(eq("tenantId"), anyString())).thenReturn(tenantQuery);
    when(tenantQuery.getSingleResult()).thenReturn("ok");
  }

  @Test
  @DisplayName("patch updates name while excluding self from uniqueness check")
  void patch_updateNameOnly_updatesNameExcludingSelf() {
    var base = createBaseEntity("System", "Original summary", null);
    var system = createSystemEntity("Old Name");
    stubActiveSystem(base, system);
    Query<UUID> nameQuery = mockQuery();
    stubExcludeNameQuery(nameQuery, List.of());

    var result = dao.patch(TENANT_ID, SYSTEM_ID, createPatchParams("New Name", null, false));

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().name()).isEqualTo("New Name");
    assertThat(result.orElseThrow().summary()).contains("Original summary");
    assertThat(result.orElseThrow().updatedOn()).isAfter(UPDATED_ON);
    var hqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(session).createQuery(hqlCaptor.capture(), eq(UUID.class));
    assertThat(hqlCaptor.getValue()).contains("s.id != :excludeId");
    verify(nameQuery).setParameter("excludeId", SYSTEM_ID);
    verify(nameQuery).setParameter("lowerName", "new name");
    verify(session).merge(base);
    verify(session).merge(system);
    verify(session).flush();
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch updates summary only without querying system name uniqueness")
  void patch_updateSummaryOnly_skipsNameUniquenessCheck() {
    var base = createBaseEntity("System", "Old summary", null);
    var system = createSystemEntity("Pathfinder");
    stubActiveSystem(base, system);

    var result = dao.patch(TENANT_ID, SYSTEM_ID, createPatchParams(null, "Updated summary", false));

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().name()).isEqualTo("Pathfinder");
    assertThat(result.orElseThrow().summary()).contains("Updated summary");
    verify(session, never()).createQuery(anyString(), eq(UUID.class));
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch updates both name and summary together")
  void patch_updateNameAndSummary_updatesBothFields() {
    var base = createBaseEntity("System", "Old summary", null);
    var system = createSystemEntity("Old System");
    stubActiveSystem(base, system);
    stubExcludeNameQuery(mockQuery(), List.of());

    var result =
        dao.patch(TENANT_ID, SYSTEM_ID, createPatchParams("Renamed", "New summary", false));

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().name()).isEqualTo("Renamed");
    assertThat(result.orElseThrow().summary()).contains("New summary");
  }

  @Test
  @DisplayName("patch clears summary to null when clearSummary is true")
  void patch_clearSummaryTrue_clearsSummaryToNull() {
    var base = createBaseEntity("System", "Existing summary", null);
    var system = createSystemEntity("Pathfinder");
    stubActiveSystem(base, system);

    var result = dao.patch(TENANT_ID, SYSTEM_ID, createPatchParams(null, null, true));

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().summary()).isEmpty();
    assertThat(base.getSummary()).isNull();
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch with empty params still refreshes updatedOn and preserves existing fields")
  void patch_emptyParams_preservesExistingFieldsAndCommits() {
    var base = createBaseEntity("System", "Kept summary", null);
    var system = createSystemEntity("Kept Name");
    stubActiveSystem(base, system);

    var result = dao.patch(TENANT_ID, SYSTEM_ID, createPatchParams(null, null, false));

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().name()).isEqualTo("Kept Name");
    assertThat(result.orElseThrow().summary()).contains("Kept summary");
    assertThat(result.orElseThrow().updatedOn()).isAfter(UPDATED_ON);
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch rejects blank name before opening database session")
  void patch_blankName_throwsIllegalArgumentWithoutOpeningSession() {
    assertThatThrownBy(() -> dao.patch(TENANT_ID, SYSTEM_ID, createPatchParams("   ", null, false)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("blank");
    verifyNoInteractions(sessionFactory);
  }

  @Test
  @DisplayName("patch returns empty when base entity is missing")
  void patch_missingBaseEntity_returnsEmptyWithoutMerge() {
    when(session.find(GameSystemEntity.class, createEntityId(SYSTEM_ID)))
        .thenReturn(createSystemEntity("Orphan"));

    var result = dao.patch(TENANT_ID, SYSTEM_ID, createPatchParams("Updated", null, false));

    assertThat(result).isEmpty();
    verify(tx).commit();
    verify(session, never()).merge(any());
  }

  @Test
  @DisplayName("patch returns empty when system subtype row is missing")
  void patch_missingSystemSubtype_returnsEmptyWithoutMerge() {
    when(session.find(EntityBaseEntity.class, createEntityId(SYSTEM_ID)))
        .thenReturn(createBaseEntity("System", null, null));

    var result = dao.patch(TENANT_ID, SYSTEM_ID, createPatchParams("Updated", null, false));

    assertThat(result).isEmpty();
    verify(tx).commit();
    verify(session, never()).merge(any());
  }

  @Test
  @DisplayName("patch returns empty when system is soft-deleted")
  void patch_softDeletedSystem_returnsEmptyWithoutMerge() {
    stubActiveSystem(
        createBaseEntity("System", null, UPDATED_ON), createSystemEntity("Deleted System"));

    var result = dao.patch(TENANT_ID, SYSTEM_ID, createPatchParams("Updated", null, false));

    assertThat(result).isEmpty();
    verify(tx).commit();
    verify(session, never()).merge(any());
  }

  @Test
  @DisplayName("patch returns empty when entity type is not System")
  void patch_wrongEntityType_returnsEmptyWithoutMerge() {
    stubActiveSystem(createBaseEntity("Event", null, null), createSystemEntity("Wrong Type"));

    var result = dao.patch(TENANT_ID, SYSTEM_ID, createPatchParams("Updated", null, false));

    assertThat(result).isEmpty();
    verify(tx).commit();
    verify(session, never()).merge(any());
  }

  @Test
  @DisplayName("patch throws IllegalStateException when new name collides in tenant")
  void patch_collidingName_throwsIllegalStateAndRollsBack() {
    stubActiveSystem(createBaseEntity("System", null, null), createSystemEntity("Original"));
    stubExcludeNameQuery(mockQuery(), List.of(OTHER_SYSTEM_ID));

    assertThatThrownBy(
            () -> dao.patch(TENANT_ID, SYSTEM_ID, createPatchParams("Taken Name", null, false)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("already exists");
    verify(tx).rollback();
    verify(session, never()).merge(any());
  }

  @Test
  @DisplayName("patch propagates ConstraintViolationException unwrapped and rolls back")
  void patch_constraintViolationOnFlush_propagatesUnwrappedAndRollsBack() {
    stubActiveSystem(createBaseEntity("System", null, null), createSystemEntity("Original"));
    stubExcludeNameQuery(mockQuery(), List.of());
    var cve = mock(ConstraintViolationException.class);
    doThrow(cve).when(session).flush();

    assertThatThrownBy(
            () -> dao.patch(TENANT_ID, SYSTEM_ID, createPatchParams("Concurrent", null, false)))
        .isSameAs(cve);
    verify(tx).rollback();
    verify(tx, never()).commit();
  }

  @Test
  @DisplayName("patch rolls back transaction and rethrows generic database errors")
  void patch_mergeThrowsException_rollsBackAndRethrows() {
    stubActiveSystem(createBaseEntity("System", null, null), createSystemEntity("Original"));
    var failure = createRuntimeException("merge failed");
    when(session.merge(any())).thenThrow(failure);

    assertThatThrownBy(
            () -> dao.patch(TENANT_ID, SYSTEM_ID, createPatchParams(null, "Summary", false)))
        .isSameAs(failure);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("softDelete marks active System entity deleted and sets tenant config")
  void softDelete_activeSystem_setsDeletedOnAndReturnsTrue() {
    var base = createBaseEntity("System", "Summary", null);
    when(session.find(EntityBaseEntity.class, createEntityId(SYSTEM_ID))).thenReturn(base);

    var deleted = dao.softDelete(TENANT_ID, SYSTEM_ID);

    assertThat(deleted).isTrue();
    assertThat(base.getDeletedOn()).isNotNull();
    assertThat(base.getUpdatedOn()).isEqualTo(base.getDeletedOn());
    verify(tenantQuery).setParameter("tenantId", TENANT_ID.toString());
    verify(session).merge(base);
    verify(tx).commit();
  }

  @Test
  @DisplayName("softDelete returns false when base entity is missing")
  void softDelete_missingEntity_returnsFalse() {
    var deleted = dao.softDelete(TENANT_ID, SYSTEM_ID);

    assertThat(deleted).isFalse();
    verify(session, never()).merge(any());
    verify(tx).commit();
  }

  @Test
  @DisplayName("softDelete returns false when entity is already soft-deleted")
  void softDelete_alreadyDeletedEntity_returnsFalse() {
    var base = createBaseEntity("System", null, UPDATED_ON);
    when(session.find(EntityBaseEntity.class, createEntityId(SYSTEM_ID))).thenReturn(base);

    var deleted = dao.softDelete(TENANT_ID, SYSTEM_ID);

    assertThat(deleted).isFalse();
    verify(session, never()).merge(any());
    verify(tx).commit();
  }

  @Test
  @DisplayName("softDelete returns false when entity type is not System")
  void softDelete_wrongEntityType_returnsFalse() {
    var base = createBaseEntity("Actor", null, null);
    when(session.find(EntityBaseEntity.class, createEntityId(SYSTEM_ID))).thenReturn(base);

    var deleted = dao.softDelete(TENANT_ID, SYSTEM_ID);

    assertThat(deleted).isFalse();
    verify(session, never()).merge(any());
    verify(tx).commit();
  }

  @Test
  @DisplayName("softDelete rolls back transaction and rethrows on database failure")
  void softDelete_mergeThrowsException_rollsBackAndRethrows() {
    var base = createBaseEntity("System", null, null);
    var failure = createRuntimeException("delete failed");
    when(session.find(EntityBaseEntity.class, createEntityId(SYSTEM_ID))).thenReturn(base);
    when(session.merge(base)).thenThrow(failure);

    assertThatThrownBy(() -> dao.softDelete(TENANT_ID, SYSTEM_ID)).isSameAs(failure);
    verify(tx).rollback();
  }

  private void stubActiveSystem(EntityBaseEntity base, GameSystemEntity system) {
    when(session.find(EntityBaseEntity.class, createEntityId(SYSTEM_ID))).thenReturn(base);
    when(session.find(GameSystemEntity.class, createEntityId(SYSTEM_ID))).thenReturn(system);
  }

  private void stubExcludeNameQuery(Query<UUID> query, List<UUID> result) {
    when(session.createQuery(anyString(), eq(UUID.class))).thenReturn(query);
    when(query.setParameter(anyString(), any())).thenReturn(query);
    when(query.getResultList()).thenReturn(result);
  }

  private static NativeQuery<String> mockNativeQuery() {
    return mock();
  }

  private static Query<UUID> mockQuery() {
    return mock();
  }

  private static DefaultGameSystemDAO createDao(SessionFactory factory) {
    return new DefaultGameSystemDAO(() -> factory);
  }

  private static EntityId createEntityId(UUID id) {
    return new EntityId(TENANT_ID, id);
  }

  private static EntityBaseEntity createBaseEntity(
      String entityType, @Nullable String summary, @Nullable Instant deletedOn) {
    return new EntityBaseEntity(
        TENANT_ID, SYSTEM_ID, GENERATOR_ID, entityType, summary, CREATED_ON, UPDATED_ON, deletedOn);
  }

  private static GameSystemEntity createSystemEntity(String name) {
    return new GameSystemEntity(TENANT_ID, SYSTEM_ID, name);
  }

  private static PatchGameSystemParams createPatchParams(
      @Nullable String name, @Nullable String summary, boolean clearSummary) {
    return new PatchGameSystemParams(
        Optional.ofNullable(name), Optional.ofNullable(summary), clearSummary);
  }

  private static RuntimeException createRuntimeException(String message) {
    return new RuntimeException(message);
  }
}
