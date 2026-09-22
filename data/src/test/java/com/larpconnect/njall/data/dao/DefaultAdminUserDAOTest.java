package com.larpconnect.njall.data.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.domain.AdminUserStatus;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class DefaultAdminUserDAOTest {

  private SessionFactory sessionFactory;
  private Session session;
  private DefaultAdminUserDAO dao;

  private final UUID userId = UUID.randomUUID();
  private final UUID roleId = UUID.randomUUID();
  private final Instant now = Instant.now();

  @BeforeEach
  void setUp() {
    sessionFactory = mock(SessionFactory.class);
    session = mock(Session.class);
    when(sessionFactory.openSession()).thenReturn(session);
    dao = new DefaultAdminUserDAO(() -> sessionFactory);
  }

  @Test
  @DisplayName("findById returns user with roles")
  void findById_success() {
    var roleEntity = new AdminRoleEntity(roleId, "auditor");
    var userEntity =
        new AdminUserEntity(
            userId, "admin_test", AdminUserStatus.ACTIVE, now, now, Set.of(roleEntity));
    when(session.find(AdminUserEntity.class, userId)).thenReturn(userEntity);

    var result = dao.findById(userId);

    assertThat(result).isPresent();
    assertThat(result.get().username()).isEqualTo("admin_test");
    assertThat(result.get().roles()).hasSize(1);
    assertThat(result.get().roles().getFirst().roleName()).isEqualTo("auditor");
  }

  @Test
  @DisplayName("findByUsername returns user with roles")
  @SuppressWarnings("unchecked")
  void findByUsername_success() {
    var roleEntity = new AdminRoleEntity(roleId, "auditor");
    var userEntity =
        new AdminUserEntity(
            userId, "admin_test", AdminUserStatus.ACTIVE, now, now, Set.of(roleEntity));
    Query<AdminUserEntity> query = mock(Query.class);
    when(session.createQuery(anyString(), eq(AdminUserEntity.class))).thenReturn(query);
    when(query.setParameter("username", "admin_test")).thenReturn(query);
    when(query.uniqueResult()).thenReturn(userEntity);

    var result = dao.findByUsername("admin_test");

    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo(userId);
  }

  @Test
  @DisplayName("list returns all users")
  @SuppressWarnings("unchecked")
  void list_success() {
    var userEntity =
        new AdminUserEntity(userId, "admin_test", AdminUserStatus.ACTIVE, now, now, Set.of());
    Query<AdminUserEntity> query = mock(Query.class);
    when(session.createQuery(anyString(), eq(AdminUserEntity.class))).thenReturn(query);
    when(query.list()).thenReturn(List.of(userEntity));

    var result = dao.list();

    assertThat(result).hasSize(1);
  }

  @Test
  @DisplayName("create inserts user and assigns roles")
  @SuppressWarnings("unchecked")
  void create_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    NativeQuery<UUID> nativeUserQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(anyString(), eq(UUID.class))).thenReturn(nativeUserQuery);
    when(nativeUserQuery.setParameter(anyString(), any())).thenReturn(nativeUserQuery);
    when(nativeUserQuery.getSingleResult()).thenReturn(userId);

    NativeQuery<Void> nativeRoleQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(anyString(), eq(Void.class))).thenReturn(nativeRoleQuery);
    when(nativeRoleQuery.setParameter(anyString(), any())).thenReturn(nativeRoleQuery);

    var userEntity =
        new AdminUserEntity(userId, "admin_test", AdminUserStatus.ACTIVE, now, now, Set.of());
    when(session.find(AdminUserEntity.class, userId)).thenReturn(userEntity);

    var result = dao.create("admin_test", AdminUserStatus.ACTIVE, List.of(roleId));

    assertThat(result.id()).isEqualTo(userId);
    verify(tx).commit();
  }

  @Test
  @DisplayName("addRole checks existence and assigns role")
  @SuppressWarnings("unchecked")
  void addRole_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);

    var userEntity =
        new AdminUserEntity(
            userId, "admin_test", AdminUserStatus.ACTIVE, now, now, new HashSet<>());
    var roleEntity = new AdminRoleEntity(roleId, "auditor");
    when(session.find(AdminUserEntity.class, userId)).thenReturn(userEntity);
    when(session.find(AdminRoleEntity.class, roleId)).thenReturn(roleEntity);

    NativeQuery<Void> nativeRoleQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(anyString(), eq(Void.class))).thenReturn(nativeRoleQuery);
    when(nativeRoleQuery.setParameter(anyString(), any())).thenReturn(nativeRoleQuery);

    var result = dao.addRole(userId, roleId);

    assertThat(result.id()).isEqualTo(userId);
    verify(tx).commit();
  }

  @Test
  @DisplayName("addRole throws IllegalArgumentException when role missing")
  void addRole_missingRole_throws() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    var userEntity =
        new AdminUserEntity(
            userId, "admin_test", AdminUserStatus.ACTIVE, now, now, new HashSet<>());
    when(session.find(AdminUserEntity.class, userId)).thenReturn(userEntity);
    when(session.find(AdminRoleEntity.class, roleId)).thenReturn(null);

    assertThatThrownBy(() -> dao.addRole(userId, roleId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Role not found");
    verify(tx).rollback();
  }

  @Test
  @DisplayName("addRole throws IllegalArgumentException when user missing")
  void addRole_missingUser_throws() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    when(session.find(AdminUserEntity.class, userId)).thenReturn(null);

    assertThatThrownBy(() -> dao.addRole(userId, roleId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("User not found");
    verify(tx).rollback();
  }

  @Test
  @DisplayName("removeRole removes assignment and returns updated user")
  @SuppressWarnings("unchecked")
  void removeRole_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    var userEntity =
        new AdminUserEntity(
            userId, "admin_test", AdminUserStatus.ACTIVE, now, now, new HashSet<>());
    var roleEntity = new AdminRoleEntity(roleId, "auditor");
    when(session.find(AdminUserEntity.class, userId)).thenReturn(userEntity);
    when(session.find(AdminRoleEntity.class, roleId)).thenReturn(roleEntity);

    NativeQuery<Void> nativeDeleteQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(anyString(), eq(Void.class))).thenReturn(nativeDeleteQuery);
    when(nativeDeleteQuery.setParameter(anyString(), any())).thenReturn(nativeDeleteQuery);

    var result = dao.removeRole(userId, roleId);

    assertThat(result.id()).isEqualTo(userId);
    verify(tx).commit();
  }

  @Test
  @DisplayName("removeRole throws IllegalArgumentException when user missing")
  void removeRole_missingUser_throws() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    when(session.find(AdminUserEntity.class, userId)).thenReturn(null);

    assertThatThrownBy(() -> dao.removeRole(userId, roleId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("User not found");
    verify(tx).rollback();
  }

  @Test
  @DisplayName("removeRole throws IllegalArgumentException when role missing")
  void removeRole_missingRole_throws() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    var userEntity =
        new AdminUserEntity(
            userId, "admin_test", AdminUserStatus.ACTIVE, now, now, new HashSet<>());
    when(session.find(AdminUserEntity.class, userId)).thenReturn(userEntity);
    when(session.find(AdminRoleEntity.class, roleId)).thenReturn(null);

    assertThatThrownBy(() -> dao.removeRole(userId, roleId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Role not found");
    verify(tx).rollback();
  }

  @Test
  @DisplayName("AdminUserEntity handles null roles set gracefully")
  void adminUserEntity_nullRolesHandled() {
    var entity = new AdminUserEntity(userId, "admin_test", AdminUserStatus.ACTIVE, now, now, null);
    assertThat(entity.getRoles()).isNotNull().isEmpty();
  }
}
