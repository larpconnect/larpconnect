package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.domain.AdminUserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

/** Package-private Hibernate entity mapping the {@code njall_admin.admin_users} table. */
@Entity
@Table(name = "admin_users", schema = "njall_admin")
class AdminUserEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "username", nullable = false, unique = true)
  private String username;

  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @Column(name = "status", nullable = false, columnDefinition = "njall_admin.tstatus")
  private AdminUserStatus status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "admin_role_assignments",
      schema = "njall_admin",
      joinColumns = @JoinColumn(name = "admin_user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  private Set<AdminRoleEntity> roles = new HashSet<>();

  AdminUserEntity() {}

  AdminUserEntity(
      UUID id,
      String username,
      AdminUserStatus status,
      Instant createdAt,
      Instant updatedAt,
      Set<AdminRoleEntity> roles) {
    this.id = id;
    this.username = username;
    this.status = status;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.roles = roles;
  }

  UUID getId() {
    return id;
  }

  void setId(UUID id) {
    this.id = id;
  }

  String getUsername() {
    return username;
  }

  void setUsername(String username) {
    this.username = username;
  }

  AdminUserStatus getStatus() {
    return status;
  }

  void setStatus(AdminUserStatus status) {
    this.status = status;
  }

  Instant getCreatedAt() {
    return createdAt;
  }

  void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  Instant getUpdatedAt() {
    return updatedAt;
  }

  void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  Set<AdminRoleEntity> getRoles() {
    return roles;
  }

  void setRoles(Set<AdminRoleEntity> roles) {
    this.roles = roles;
  }
}
