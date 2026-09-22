package com.larpconnect.njall.data.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

final class DefaultAdminRoleDAOTest {

  private SessionFactory sessionFactory;
  private Session session;
  private DefaultAdminRoleDAO dao;

  private final UUID roleId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    sessionFactory = mock(SessionFactory.class);
    session = mock(Session.class);
    when(sessionFactory.openSession()).thenReturn(session);
    dao = new DefaultAdminRoleDAO(() -> sessionFactory);
  }

  @Test
  @DisplayName("findById returns role when present")
  void findById_whenPresent_returnsRole() {
    var entity = new AdminRoleEntity(roleId, "auditor");
    when(session.find(AdminRoleEntity.class, roleId)).thenReturn(entity);

    var result = dao.findById(roleId);

    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo(roleId);
    assertThat(result.get().roleName()).isEqualTo("auditor");
  }

  @Test
  @DisplayName("findById returns empty when not found")
  void findById_whenNotFound_returnsEmpty() {
    when(session.find(AdminRoleEntity.class, roleId)).thenReturn(null);

    var result = dao.findById(roleId);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("findByRoleName returns role when present")
  @SuppressWarnings("unchecked")
  void findByRoleName_whenPresent_returnsRole() {
    var entity = new AdminRoleEntity(roleId, "auditor");
    Query<AdminRoleEntity> query = mock(Query.class);
    when(session.createQuery(
            "from AdminRoleEntity where roleName = :roleName", AdminRoleEntity.class))
        .thenReturn(query);
    when(query.setParameter("roleName", "auditor")).thenReturn(query);
    when(query.uniqueResult()).thenReturn(entity);

    var result = dao.findByRoleName("auditor");

    assertThat(result).isPresent();
    assertThat(result.get().roleName()).isEqualTo("auditor");
  }

  @Test
  @DisplayName("list returns all roles ordered")
  @SuppressWarnings("unchecked")
  void list_returnsRoles() {
    var entity = new AdminRoleEntity(roleId, "auditor");
    Query<AdminRoleEntity> query = mock(Query.class);
    when(session.createQuery("from AdminRoleEntity order by roleName asc", AdminRoleEntity.class))
        .thenReturn(query);
    when(query.list()).thenReturn(List.of(entity));

    var result = dao.list();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().roleName()).isEqualTo("auditor");
  }

  @Test
  @DisplayName("create inserts and returns role")
  @SuppressWarnings("unchecked")
  void create_success_returnsRole() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    NativeQuery<UUID> nativeQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(anyString(), eq(UUID.class))).thenReturn(nativeQuery);
    when(nativeQuery.setParameter(eq("roleName"), any())).thenReturn(nativeQuery);
    when(nativeQuery.getSingleResult()).thenReturn(roleId);

    var role = dao.create("auditor");

    assertThat(role.id()).isEqualTo(roleId);
    assertThat(role.roleName()).isEqualTo("auditor");
    verify(tx).commit();
  }

  @Test
  @DisplayName("create rolls back transaction on error")
  @SuppressWarnings("unchecked")
  void create_onException_rollsBack() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    NativeQuery<UUID> nativeQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(anyString(), eq(UUID.class))).thenReturn(nativeQuery);
    when(nativeQuery.setParameter(eq("roleName"), any())).thenReturn(nativeQuery);
    when(nativeQuery.getSingleResult()).thenThrow(new RuntimeException("DB error"));

    assertThatThrownBy(() -> dao.create("auditor"))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("DB error");
    verify(tx).rollback();
  }
}
