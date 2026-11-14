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
import com.larpconnect.njall.data.domain.studios.GameSystem;
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

final class DefaultGameSystemDAOTest {

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
  @DisplayName("findById returns active GameSystem with summary and sets tenant config")
  void findById_activeSystemWithSummary_returnsSystem() {
    var base = createBaseEntity(SYSTEM_ID, "System", "TTRPG ruleset", null);
    var system = createSystemEntity(SYSTEM_ID, "Dungeons & Dragons");
    when(session.find(EntityBaseEntity.class, createEntityId(SYSTEM_ID))).thenReturn(base);
    when(session.find(GameSystemEntity.class, createEntityId(SYSTEM_ID))).thenReturn(system);

    var result = dao.findById(TENANT_ID, SYSTEM_ID);

    assertThat(result)
        .contains(
            createExpectedSystem(
                SYSTEM_ID, "Dungeons & Dragons", Optional.of("TTRPG ruleset"), CREATED_ON));
    verify(tenantQuery).setParameter("tenantId", TENANT_ID.toString());
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns GameSystem with empty optional summary when null")
  void findById_activeSystemWithoutSummary_returnsEmptySummary() {
    var base = createBaseEntity(SYSTEM_ID, "System", null, null);
    var system = createSystemEntity(SYSTEM_ID, "Fate Core");
    when(session.find(EntityBaseEntity.class, createEntityId(SYSTEM_ID))).thenReturn(base);
    when(session.find(GameSystemEntity.class, createEntityId(SYSTEM_ID))).thenReturn(system);

    var result = dao.findById(TENANT_ID, SYSTEM_ID);

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().summary()).isEmpty();
  }

  @Test
  @DisplayName("findById returns empty when base entity does not exist")
  void findById_missingBaseEntity_returnsEmpty() {
    var system = createSystemEntity(SYSTEM_ID, "Fate Core");
    when(session.find(GameSystemEntity.class, createEntityId(SYSTEM_ID))).thenReturn(system);

    var result = dao.findById(TENANT_ID, SYSTEM_ID);

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns empty when system subtype row does not exist")
  void findById_missingSystemSubtype_returnsEmpty() {
    var base = createBaseEntity(SYSTEM_ID, "System", null, null);
    when(session.find(EntityBaseEntity.class, createEntityId(SYSTEM_ID))).thenReturn(base);

    var result = dao.findById(TENANT_ID, SYSTEM_ID);

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns empty when system is soft-deleted")
  void findById_softDeletedEntity_returnsEmpty() {
    var base = createBaseEntity(SYSTEM_ID, "System", null, UPDATED_ON);
    var system = createSystemEntity(SYSTEM_ID, "Deleted System");
    when(session.find(EntityBaseEntity.class, createEntityId(SYSTEM_ID))).thenReturn(base);
    when(session.find(GameSystemEntity.class, createEntityId(SYSTEM_ID))).thenReturn(system);

    var result = dao.findById(TENANT_ID, SYSTEM_ID);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("findById returns empty when entity type is not System")
  void findById_wrongEntityType_returnsEmpty() {
    var base = createBaseEntity(SYSTEM_ID, "Event", null, null);
    var system = createSystemEntity(SYSTEM_ID, "Mismatch");
    when(session.find(EntityBaseEntity.class, createEntityId(SYSTEM_ID))).thenReturn(base);
    when(session.find(GameSystemEntity.class, createEntityId(SYSTEM_ID))).thenReturn(system);

    var result = dao.findById(TENANT_ID, SYSTEM_ID);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("findById rolls back transaction and rethrows on database failure")
  void findById_sessionThrows_rollsBackAndRethrows() {
    var failure = createRuntimeException("db error");
    when(session.find(EntityBaseEntity.class, createEntityId(SYSTEM_ID))).thenThrow(failure);

    assertThatThrownBy(() -> dao.findById(TENANT_ID, SYSTEM_ID)).isSameAs(failure);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("findById attaches suppressed rollback failure when rollback also throws")
  void findById_rollbackThrows_addsSuppressedException() {
    var failure = createRuntimeException("query error");
    var rollbackFailure = createRuntimeException("rollback error");
    when(session.find(EntityBaseEntity.class, createEntityId(SYSTEM_ID))).thenThrow(failure);
    doThrow(rollbackFailure).when(tx).rollback();

    assertThatThrownBy(() -> dao.findById(TENANT_ID, SYSTEM_ID))
        .isSameAs(failure)
        .hasSuppressedException(rollbackFailure);
  }

  @Test
  @DisplayName("listAll queries active System rows ordered by lower(name) asc and createdOn desc")
  void listAll_activeSystems_returnsMappedResultsWithExpectedQueryParams() {
    var query = mockRowQuery();
    var firstBase = createBaseEntity(SYSTEM_ID, "System", "Alpha summary", null);
    var firstSys = createSystemEntity(SYSTEM_ID, "Alpha");
    var secondBase = createBaseEntity(OTHER_SYSTEM_ID, "System", null, null);
    var secondSys = createSystemEntity(OTHER_SYSTEM_ID, "Zeta");
    when(session.createQuery(anyString(), eq(Object[].class))).thenReturn(query);
    when(query.setParameter(anyString(), any())).thenReturn(query);
    when(query.getResultList())
        .thenReturn(List.of(createRow(firstBase, firstSys), createRow(secondBase, secondSys)));

    var results = dao.listAll(TENANT_ID);

    assertThat(results)
        .containsExactly(
            createExpectedSystem(SYSTEM_ID, "Alpha", Optional.of("Alpha summary"), CREATED_ON),
            createExpectedSystem(OTHER_SYSTEM_ID, "Zeta", Optional.empty(), CREATED_ON));
    verify(query).setParameter("tenantId", TENANT_ID);
    verify(query).setParameter("entityType", "System");
    verify(tx).commit();
  }

  @Test
  @DisplayName("listAll returns empty immutable list when no systems exist")
  void listAll_noSystems_returnsEmptyList() {
    var query = mockRowQuery();
    when(session.createQuery(anyString(), eq(Object[].class))).thenReturn(query);
    when(query.setParameter(anyString(), any())).thenReturn(query);
    when(query.getResultList()).thenReturn(List.of());

    var results = dao.listAll(TENANT_ID);

    assertThat(results).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("listAll rolls back transaction and rethrows on query failure")
  void listAll_queryThrows_rollsBackAndRethrows() {
    var query = mockRowQuery();
    var failure = createRuntimeException("list failure");
    when(session.createQuery(anyString(), eq(Object[].class))).thenReturn(query);
    when(query.setParameter(anyString(), any())).thenReturn(query);
    when(query.getResultList()).thenThrow(failure);

    assertThatThrownBy(() -> dao.listAll(TENANT_ID)).isSameAs(failure);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("create persists EntityBaseEntity and GameSystemEntity with valid active actor")
  void create_validActorAndUniqueName_persistsAndReturnsSystem() {
    stubValidGenerator();
    var nameQuery = mockUuidQuery();
    stubUniqueNameQuery(nameQuery, List.of());

    var created =
        dao.create(TENANT_ID, GENERATOR_ID, "Pathfinder 2e", Optional.of("Remastered rules"));

    var baseCaptor = ArgumentCaptor.forClass(EntityBaseEntity.class);
    var systemCaptor = ArgumentCaptor.forClass(GameSystemEntity.class);
    verify(session).persist(baseCaptor.capture());
    verify(session).persist(systemCaptor.capture());
    verify(session).flush();
    verify(tx).commit();
    assertThat(created.id()).isEqualTo(baseCaptor.getValue().getId());
    assertThat(created.generator()).isEqualTo(GENERATOR_ID);
    assertThat(created.name()).isEqualTo("Pathfinder 2e");
    assertThat(created.summary()).contains("Remastered rules");
    assertThat(baseCaptor.getValue().getEntityType()).isEqualTo("System");
    assertThat(systemCaptor.getValue().getName()).isEqualTo("Pathfinder 2e");
    verify(nameQuery).setParameter("lowerName", "pathfinder 2e");
  }

  @Test
  @DisplayName("create persists system with empty optional summary")
  void create_withoutSummary_persistsNullSummary() {
    stubValidGenerator();
    stubUniqueNameQuery(mockUuidQuery(), List.of());

    var created = dao.create(TENANT_ID, GENERATOR_ID, "Blades in the Dark", Optional.empty());

    assertThat(created.summary()).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("create rejects whitespace-only name without opening session")
  void create_blankName_throwsIllegalArgumentWithoutOpeningSession() {
    assertThatThrownBy(() -> dao.create(TENANT_ID, GENERATOR_ID, "   ", Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("blank");
    verifyNoInteractions(sessionFactory);
  }

  @Test
  @DisplayName("create rejects empty name without opening session")
  void create_emptyName_throwsIllegalArgumentWithoutOpeningSession() {
    assertThatThrownBy(() -> dao.create(TENANT_ID, GENERATOR_ID, "", Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(sessionFactory);
  }

  @Test
  @DisplayName("create rejects missing generator actor and rolls back transaction")
  void create_missingGenerator_throwsIllegalArgumentAndRollsBack() {
    assertThatThrownBy(() -> dao.create(TENANT_ID, GENERATOR_ID, "GURPS", Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class);
    verify(tx).rollback();
    verify(session, never()).persist(any());
  }

  @Test
  @DisplayName("create rejects soft-deleted generator actor and rolls back transaction")
  void create_deletedGenerator_throwsIllegalArgumentAndRollsBack() {
    var deletedActor = createBaseEntity(GENERATOR_ID, "Actor", null, UPDATED_ON);
    when(session.find(EntityBaseEntity.class, createEntityId(GENERATOR_ID)))
        .thenReturn(deletedActor);

    assertThatThrownBy(() -> dao.create(TENANT_ID, GENERATOR_ID, "GURPS", Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("create rejects non-Actor generator and rolls back transaction")
  void create_nonActorGenerator_throwsIllegalArgumentAndRollsBack() {
    var wrongType = createBaseEntity(GENERATOR_ID, "System", null, null);
    when(session.find(EntityBaseEntity.class, createEntityId(GENERATOR_ID))).thenReturn(wrongType);

    assertThatThrownBy(() -> dao.create(TENANT_ID, GENERATOR_ID, "GURPS", Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("create throws IllegalStateException on tenant-wide case-insensitive name collision")
  void create_duplicateNameCaseInsensitive_throwsIllegalStateAndRollsBack() {
    stubValidGenerator();
    var nameQuery = mockUuidQuery();
    stubUniqueNameQuery(nameQuery, List.of(OTHER_SYSTEM_ID));

    assertThatThrownBy(() -> dao.create(TENANT_ID, GENERATOR_ID, "GURPS", Optional.empty()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("already exists");
    verify(nameQuery).setParameter("lowerName", "gurps");
    verify(tx).rollback();
    verify(session, never()).persist(any());
  }

  @Test
  @DisplayName("create propagates ConstraintViolationException unwrapped and rolls back")
  void create_constraintViolationOnFlush_propagatesUnwrappedAndRollsBack() {
    stubValidGenerator();
    stubUniqueNameQuery(mockUuidQuery(), List.of());
    var cve = mock(ConstraintViolationException.class);
    doThrow(cve).when(session).flush();

    assertThatThrownBy(() -> dao.create(TENANT_ID, GENERATOR_ID, "Daggerheart", Optional.empty()))
        .isSameAs(cve);
    verify(tx).rollback();
    verify(tx, never()).commit();
  }

  @Test
  @DisplayName("create rolls back and rethrows generic persistence errors")
  void create_genericPersistenceError_rollsBackAndRethrows() {
    stubValidGenerator();
    stubUniqueNameQuery(mockUuidQuery(), List.of());
    var failure = createRuntimeException("persist failed");
    doThrow(failure).when(session).flush();

    assertThatThrownBy(() -> dao.create(TENANT_ID, GENERATOR_ID, "Daggerheart", Optional.empty()))
        .isSameAs(failure);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("Untenanted DAO.findById(UUID) throws UnsupportedOperationException")
  void untenantedFindById_throwsUnsupportedOperation() {
    GameSystemDAO contract = dao;
    assertThatThrownBy(() -> contract.findById(SYSTEM_ID))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Untenanted DAO.list() throws UnsupportedOperationException")
  void untenantedList_throwsUnsupportedOperation() {
    GameSystemDAO contract = dao;
    assertThatThrownBy(contract::list).isInstanceOf(UnsupportedOperationException.class);
  }

  private void stubValidGenerator() {
    var actor = createBaseEntity(GENERATOR_ID, "Actor", null, null);
    when(session.find(EntityBaseEntity.class, createEntityId(GENERATOR_ID))).thenReturn(actor);
  }

  private void stubUniqueNameQuery(Query<UUID> query, List<UUID> result) {
    when(session.createQuery(anyString(), eq(UUID.class))).thenReturn(query);
    when(query.setParameter(anyString(), any())).thenReturn(query);
    when(query.getResultList()).thenReturn(result);
  }

  private static NativeQuery<String> mockNativeQuery() {
    return mock();
  }

  private static Query<Object[]> mockRowQuery() {
    return mock();
  }

  private static Query<UUID> mockUuidQuery() {
    return mock();
  }

  private static DefaultGameSystemDAO createDao(SessionFactory factory) {
    return new DefaultGameSystemDAO(() -> factory);
  }

  private static EntityId createEntityId(UUID id) {
    return new EntityId(TENANT_ID, id);
  }

  private static EntityBaseEntity createBaseEntity(
      UUID id, String entityType, @Nullable String summary, @Nullable Instant deletedOn) {
    return new EntityBaseEntity(
        TENANT_ID, id, GENERATOR_ID, entityType, summary, CREATED_ON, UPDATED_ON, deletedOn);
  }

  private static GameSystemEntity createSystemEntity(UUID id, String name) {
    return new GameSystemEntity(TENANT_ID, id, name);
  }

  private static GameSystem createExpectedSystem(
      UUID id, String name, Optional<String> summary, Instant createdOn) {
    return new GameSystem(id, GENERATOR_ID, name, summary, createdOn, UPDATED_ON, Optional.empty());
  }

  private static Object[] createRow(EntityBaseEntity base, GameSystemEntity system) {
    return new Object[] {base, system};
  }

  private static RuntimeException createRuntimeException(String message) {
    return new RuntimeException(message);
  }
}
