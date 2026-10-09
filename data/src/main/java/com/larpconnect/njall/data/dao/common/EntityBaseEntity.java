package com.larpconnect.njall.data.dao.common;

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
public class EntityBaseEntity {

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

  public EntityBaseEntity() {}

  public EntityBaseEntity(
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

  public UUID getTenantId() {
    return tenantId;
  }

  public UUID getId() {
    return id;
  }

  public String getEntityType() {
    return entityType;
  }

  public @Nullable String getSummary() {
    return summary;
  }

  public void setSummary(@Nullable String summary) {
    this.summary = summary;
  }

  public Instant getCreatedOn() {
    return createdOn;
  }

  public Instant getUpdatedOn() {
    return updatedOn;
  }

  public void setUpdatedOn(Instant updatedOn) {
    this.updatedOn = updatedOn;
  }

  public @Nullable Instant getDeletedOn() {
    return deletedOn;
  }

  public void setDeletedOn(@Nullable Instant deletedOn) {
    this.deletedOn = deletedOn;
  }
}
