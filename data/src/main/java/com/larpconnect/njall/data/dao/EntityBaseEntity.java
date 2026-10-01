package com.larpconnect.njall.data.dao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** JPA entity mapping the {@code njall_users.entities} Common Table Inheritance root table. */
@Entity
@Table(name = "entities", schema = "njall_users")
@IdClass(EntityId.class)
class EntityBaseEntity {

  @Id
  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "entity_type", nullable = false, updatable = false)
  private String entityType;

  @Column(name = "summary")
  private @Nullable String summary;

  @Column(name = "created_on", nullable = false, updatable = false)
  private Instant createdOn;

  @Column(name = "updated_on", nullable = false)
  private Instant updatedOn;

  @Column(name = "deleted_on")
  private @Nullable Instant deletedOn;

  EntityBaseEntity() {}

  EntityBaseEntity(
      UUID tenantId,
      UUID id,
      String entityType,
      @Nullable String summary,
      Instant createdOn,
      Instant updatedOn,
      @Nullable Instant deletedOn) {
    this.tenantId = tenantId;
    this.id = id;
    this.entityType = entityType;
    this.summary = summary;
    this.createdOn = createdOn;
    this.updatedOn = updatedOn;
    this.deletedOn = deletedOn;
  }

  UUID getTenantId() {
    return tenantId;
  }

  UUID getId() {
    return id;
  }

  String getEntityType() {
    return entityType;
  }

  @Nullable String getSummary() {
    return summary;
  }

  void setSummary(@Nullable String summary) {
    this.summary = summary;
  }

  Instant getCreatedOn() {
    return createdOn;
  }

  Instant getUpdatedOn() {
    return updatedOn;
  }

  void setUpdatedOn(Instant updatedOn) {
    this.updatedOn = updatedOn;
  }

  @Nullable Instant getDeletedOn() {
    return deletedOn;
  }

  void setDeletedOn(@Nullable Instant deletedOn) {
    this.deletedOn = deletedOn;
  }
}
