package com.larpconnect.njall.data.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.domain.DeletionFilter;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class DefaultStudioLookupDAOTest {

  private SessionFactory sessionFactory;
  private Session session;
  private DefaultStudioLookupDAO dao;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID studioId = UUID.randomUUID();
  private final Instant now = Instant.now();

  @BeforeEach
  void setUp() {
    sessionFactory = mock(SessionFactory.class);
    session = mock(Session.class);
    when(sessionFactory.openSession()).thenReturn(session);
    dao = new DefaultStudioLookupDAO(() -> sessionFactory);
  }

  @Test
  @DisplayName("findById filters soft-deleted by default and includes when requested")
  // Suppressed due to Hibernate generic query mocking type erasure.
  @SuppressWarnings("unchecked")
  void findById_filtering() {
    var entity = new StudioLookupEntity(tenantId, studioId, "valhalla", now, now, null);
    Query<StudioLookupEntity> query = mock(Query.class);
    when(session.createQuery(
            contains("where studioId = :studioId and deletedAt is null"),
            eq(StudioLookupEntity.class)))
        .thenReturn(query);
    when(query.setParameter("studioId", studioId)).thenReturn(query);
    when(query.uniqueResult()).thenReturn(entity);

    var result = dao.findById(studioId);

    assertThat(result).isPresent();
    assertThat(result.get().alias()).isEqualTo("valhalla");

    var resultActive = dao.findById(studioId, DeletionFilter.ACTIVE_ONLY);
    assertThat(resultActive).isPresent();

    Query<StudioLookupEntity> queryAll = mock(Query.class);
    when(session.createQuery(
            "from StudioLookupEntity where studioId = :studioId", StudioLookupEntity.class))
        .thenReturn(queryAll);
    when(queryAll.setParameter("studioId", studioId)).thenReturn(queryAll);
    when(queryAll.uniqueResult()).thenReturn(entity);

    var resultAll = dao.findById(studioId, DeletionFilter.INCLUDE_DELETED);
    assertThat(resultAll).isPresent();
  }

  @Test
  @DisplayName("findByAlias filters soft-deleted by default and includes when requested")
  // Suppressed due to Hibernate generic query mocking type erasure.
  @SuppressWarnings("unchecked")
  void findByAlias_filtering() {
    var entity = new StudioLookupEntity(tenantId, studioId, "valhalla", now, now, null);
    Query<StudioLookupEntity> query = mock(Query.class);
    when(session.createQuery(
            contains("where alias = :alias and deletedAt is null"), eq(StudioLookupEntity.class)))
        .thenReturn(query);
    when(query.setParameter("alias", "valhalla")).thenReturn(query);
    when(query.uniqueResult()).thenReturn(entity);

    var result = dao.findByAlias("valhalla");

    assertThat(result).isPresent();
    assertThat(result.get().studioId()).isEqualTo(studioId);

    var resultActive = dao.findByAlias("valhalla", DeletionFilter.ACTIVE_ONLY);
    assertThat(resultActive).isPresent();

    Query<StudioLookupEntity> queryAll = mock(Query.class);
    when(session.createQuery(
            "from StudioLookupEntity where alias = :alias", StudioLookupEntity.class))
        .thenReturn(queryAll);
    when(queryAll.setParameter("alias", "valhalla")).thenReturn(queryAll);
    when(queryAll.uniqueResult()).thenReturn(entity);

    var resultAll = dao.findByAlias("valhalla", DeletionFilter.INCLUDE_DELETED);
    assertThat(resultAll).isPresent();
  }

  @Test
  @DisplayName("list returns studios according to DeletionFilter")
  // Suppressed due to Hibernate generic query mocking type erasure.
  @SuppressWarnings("unchecked")
  void list_filtering() {
    var entity = new StudioLookupEntity(tenantId, studioId, "valhalla", now, now, null);
    Query<StudioLookupEntity> query = mock(Query.class);
    when(session.createQuery(
            contains("where deletedAt is null order by alias asc"), eq(StudioLookupEntity.class)))
        .thenReturn(query);
    when(query.list()).thenReturn(List.of(entity));

    var list = dao.list();
    assertThat(list).hasSize(1);

    var listActive = dao.list(DeletionFilter.ACTIVE_ONLY);
    assertThat(listActive).hasSize(1);

    Query<StudioLookupEntity> queryAll = mock(Query.class);
    when(session.createQuery(
            "from StudioLookupEntity order by alias asc", StudioLookupEntity.class))
        .thenReturn(queryAll);
    when(queryAll.list()).thenReturn(List.of(entity));

    var listAll = dao.list(DeletionFilter.INCLUDE_DELETED);
    assertThat(listAll).hasSize(1);
  }

  @Test
  @DisplayName("toInstant converts Instant, OffsetDateTime, Timestamp, and rejects others")
  void toInstant_convertsVariousTypes() {
    assertThat(DefaultStudioLookupDAO.toInstant(now)).isEqualTo(now);

    var odt = OffsetDateTime.now(ZoneOffset.UTC);
    assertThat(DefaultStudioLookupDAO.toInstant(odt)).isEqualTo(odt.toInstant());

    var ts = new Timestamp(1000L);
    assertThat(DefaultStudioLookupDAO.toInstant(ts)).isEqualTo(ts.toInstant());

    assertThatThrownBy(() -> DefaultStudioLookupDAO.toInstant("invalid-timestamp"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("create inserts user studio and admin lookup, returning StudioLookup")
  // Suppressed due to Hibernate generic query mocking type erasure.
  @SuppressWarnings("unchecked")
  void create_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);

    NativeQuery<UUID> studioQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("njall_users.studios"), eq(UUID.class)))
        .thenReturn(studioQuery);
    when(studioQuery.setParameter(eq("name"), any())).thenReturn(studioQuery);
    when(studioQuery.getSingleResult()).thenReturn(tenantId);

    NativeQuery<Object[]> lookupQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("njall_admin.studios_lookup"), eq(Object[].class)))
        .thenReturn(lookupQuery);
    when(lookupQuery.setParameter(eq("tenantId"), any())).thenReturn(lookupQuery);
    when(lookupQuery.setParameter(eq("alias"), any())).thenReturn(lookupQuery);
    when(lookupQuery.getSingleResult()).thenReturn(new Object[] {studioId, now, now});

    var result = dao.create("valhalla", "Valhalla Studio");

    assertThat(result.tenantId()).isEqualTo(tenantId);
    assertThat(result.studioId()).isEqualTo(studioId);
    assertThat(result.alias()).isEqualTo("valhalla");
    verify(tx).commit();
  }

  @Test
  @DisplayName("create rollbacks on exception")
  // Suppressed due to Hibernate generic query mocking type erasure.
  @SuppressWarnings("unchecked")
  void create_rollsBackOnException() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);

    NativeQuery<UUID> studioQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("njall_users.studios"), eq(UUID.class)))
        .thenReturn(studioQuery);
    when(studioQuery.setParameter(eq("name"), any())).thenReturn(studioQuery);
    when(studioQuery.getSingleResult()).thenThrow(new RuntimeException("DB error"));

    assertThatThrownBy(() -> dao.create("valhalla", "Valhalla Studio"))
        .isInstanceOf(RuntimeException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("softDelete marks deletedAt and updates record")
  // Suppressed due to Hibernate generic query mocking type erasure.
  @SuppressWarnings("unchecked")
  void softDelete_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    var entity = new StudioLookupEntity(tenantId, studioId, "valhalla", now, now, null);
    Query<StudioLookupEntity> query = mock(Query.class);
    when(session.createQuery(anyString(), eq(StudioLookupEntity.class))).thenReturn(query);
    when(query.setParameter("studioId", studioId)).thenReturn(query);
    when(query.uniqueResult()).thenReturn(entity);

    var result = dao.softDelete(studioId);

    assertThat(result).isPresent();
    assertThat(result.get().isDeleted()).isTrue();
    verify(tx).commit();
  }

  @Test
  @DisplayName("softDelete returns empty when not found")
  // Suppressed due to Hibernate generic query mocking type erasure.
  @SuppressWarnings("unchecked")
  void softDelete_notFound() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    Query<StudioLookupEntity> query = mock(Query.class);
    when(session.createQuery(anyString(), eq(StudioLookupEntity.class))).thenReturn(query);
    when(query.setParameter("studioId", studioId)).thenReturn(query);
    when(query.uniqueResult()).thenReturn(null);

    var result = dao.softDelete(studioId);

    assertThat(result).isEmpty();
    verify(tx).rollback();
  }
}
