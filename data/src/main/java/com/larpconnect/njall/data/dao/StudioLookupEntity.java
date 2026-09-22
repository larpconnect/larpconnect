package com.larpconnect.njall.data.dao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Package-private Hibernate entity mapping the {@code njall_admin.studios_lookup} table. */
@Entity
@Table(name = "studios_lookup", schema = "njall_admin")
class StudioLookupEntity {

  @Id
  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Column(name = "studio_id", nullable = false, updatable = false, unique = true)
  private UUID studioId;

  @Column(name = "alias", nullable = false, unique = true)
  private String alias;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "deleted_at")
  private @Nullable Instant deletedAt;

  StudioLookupEntity() {}

  StudioLookupEntity(
      UUID tenantId,
      UUID studioId,
      String alias,
      Instant createdAt,
      Instant updatedAt,
      @Nullable Instant deletedAt) {
    this.tenantId = tenantId;
    this.studioId = studioId;
    this.alias = alias;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.deletedAt = deletedAt;
  }

  UUID getTenantId() {
    return tenantId;
  }

  void setTenantId(UUID tenantId) {
    this.tenantId = tenantId;
  }

  UUID getStudioId() {
    return studioId;
  }

  void setStudioId(UUID studioId) {
    this.studioId = studioId;
  }

  String getAlias() {
    return alias;
  }

  void setAlias(String alias) {
    this.alias = alias;
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

  @Nullable Instant getDeletedAt() {
    return deletedAt;
  }

  void setDeletedAt(@Nullable Instant deletedAt) {
    this.deletedAt = deletedAt;
  }
}
