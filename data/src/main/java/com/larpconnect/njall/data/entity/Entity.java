package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Base class for all independent entity objects in the system. */
@jakarta.persistence.Entity
@Table(name = "entities")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "entity_type")
public abstract non-sealed class Entity implements DatabaseObject {

  @Id
  @Column(name = "id")
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "external_id")
  private ExternalResource externalResource;

  @Column(name = "created_on", nullable = false, insertable = false, updatable = false)
  private OffsetDateTime createdOn;

  @Column(name = "updated_on", nullable = false, insertable = false, updatable = false)
  private OffsetDateTime updatedOn;

  @Column(name = "deleted_on")
  private OffsetDateTime deletedOn;

  protected Entity() {}

  @Override
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public ExternalResource getExternalResource() {
    return externalResource;
  }

  public void setExternalResource(ExternalResource externalResource) {
    this.externalResource = externalResource;
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
