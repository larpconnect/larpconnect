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

final class DefaultStudioRoleDAOTest {

  private SessionFactory sessionFactory;
  private Session session;
  private DefaultDefaultStudioRoleDAO dao;

  private final UUID roleId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    sessionFactory = mock(SessionFactory.class);
    session = mock(Session.class);
    when(sessionFactory.openSession()).thenReturn(session);
    dao = new DefaultDefaultStudioRoleDAO(() -> sessionFactory);
  }

  @Test
  @DisplayName("findById returns role when present")
  void findById_success() {
    var entity = new DefaultStudioRoleEntity(roleId, "ORGANIZER");
    when(session.find(DefaultStudioRoleEntity.class, roleId)).thenReturn(entity);

    var result = dao.findById(roleId);

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().id()).isEqualTo(roleId);
    assertThat(result.orElseThrow().name()).isEqualTo("ORGANIZER");
  }

  @Test
  @DisplayName("findByName returns role when present")
  // Suppressed due to Hibernate generic query mocking type erasure.
  @SuppressWarnings("unchecked")
  void findByName_success() {
    var entity = new DefaultStudioRoleEntity(roleId, "ORGANIZER");
    Query<DefaultStudioRoleEntity> query = mock(Query.class);
    when(session.createQuery(contains("where name = :name"), eq(DefaultStudioRoleEntity.class)))
        .thenReturn(query);
    when(query.setParameter("name", "ORGANIZER")).thenReturn(query);
    when(query.uniqueResult()).thenReturn(entity);

    var result = dao.findByName("ORGANIZER");

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().id()).isEqualTo(roleId);
    assertThat(result.orElseThrow().name()).isEqualTo("ORGANIZER");
  }

  @Test
  @DisplayName("list returns sorted default studio roles")
  // Suppressed due to Hibernate generic query mocking type erasure.
  @SuppressWarnings("unchecked")
  void list_success() {
    var entity = new DefaultStudioRoleEntity(roleId, "ORGANIZER");
    Query<DefaultStudioRoleEntity> query = mock(Query.class);
    when(session.createQuery(contains("order by name asc"), eq(DefaultStudioRoleEntity.class)))
        .thenReturn(query);
    when(query.list()).thenReturn(List.of(entity));

    var list = dao.list();

    assertThat(list).hasSize(1);
    assertThat(list.getFirst().name()).isEqualTo("ORGANIZER");
  }

  @Test
  @DisplayName("create persists new default studio role")
  // Suppressed due to Hibernate generic query mocking type erasure.
  @SuppressWarnings("unchecked")
  void create_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);

    NativeQuery<UUID> nativeQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(anyString(), eq(UUID.class))).thenReturn(nativeQuery);
    when(nativeQuery.setParameter(eq("name"), any())).thenReturn(nativeQuery);
    when(nativeQuery.getSingleResult()).thenReturn(roleId);

    var result = dao.create("ORGANIZER");

    assertThat(result.id()).isEqualTo(roleId);
    assertThat(result.name()).isEqualTo("ORGANIZER");
    verify(tx).commit();
  }

  @Test
  @DisplayName("create rollbacks on exception")
  // Suppressed due to Hibernate generic query mocking type erasure.
  @SuppressWarnings("unchecked")
  void create_rollsBackOnException() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);

    NativeQuery<UUID> nativeQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(anyString(), eq(UUID.class))).thenReturn(nativeQuery);
    when(nativeQuery.setParameter(eq("name"), any())).thenReturn(nativeQuery);
    when(nativeQuery.getSingleResult()).thenThrow(new RuntimeException("DB error"));

    assertThatThrownBy(() -> dao.create("ORGANIZER")).isInstanceOf(RuntimeException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("update modifies name and returns updated role")
  void update_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    var entity = new DefaultStudioRoleEntity(roleId, "ORGANIZER");
    when(session.find(DefaultStudioRoleEntity.class, roleId)).thenReturn(entity);

    var result = dao.update(roleId, "LEAD_ORGANIZER");

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().name()).isEqualTo("LEAD_ORGANIZER");
    verify(session).merge(entity);
    verify(tx).commit();
  }

  @Test
  @DisplayName("update returns empty when role not found")
  void update_notFound() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    when(session.find(DefaultStudioRoleEntity.class, roleId)).thenReturn(null);

    var result = dao.update(roleId, "LEAD_ORGANIZER");

    assertThat(result).isEmpty();
    verify(tx).rollback();
  }
}
