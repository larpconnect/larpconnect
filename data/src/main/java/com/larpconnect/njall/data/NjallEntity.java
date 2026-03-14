package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Base entity for the Njall data model. */
@Entity
@Table(name = "entities")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class NjallEntity {

  private static final int EXTERNAL_REFERENCE_LENGTH = 2048;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "entity_type", nullable = false)
  private String entityType;

  @Column(name = "external_reference", length = EXTERNAL_REFERENCE_LENGTH)
  private String externalReference;

  @Column(name = "created_on", nullable = false, updatable = false)
  private OffsetDateTime createdOn = OffsetDateTime.now(java.time.ZoneOffset.UTC);

  @Column(name = "updated_on", nullable = false)
  private OffsetDateTime updatedOn = OffsetDateTime.now(java.time.ZoneOffset.UTC);

  @Column(name = "deleted_on")
  private OffsetDateTime deletedOn;

  public NjallEntity() {}

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getEntityType() {
    return entityType;
  }

  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }

  public String getExternalReference() {
    return externalReference;
  }

  public void setExternalReference(String externalReference) {
    this.externalReference = externalReference;
  }

  public OffsetDateTime getCreatedOn() {
    return createdOn;
  }

  public void setCreatedOn(OffsetDateTime createdOn) {
    this.createdOn = createdOn;
  }

  public OffsetDateTime getUpdatedOn() {
    return updatedOn;
  }

  public void setUpdatedOn(OffsetDateTime updatedOn) {
    this.updatedOn = updatedOn;
  }

  public OffsetDateTime getDeletedOn() {
    return deletedOn;
  }

  public void setDeletedOn(OffsetDateTime deletedOn) {
    this.deletedOn = deletedOn;
  }
}
