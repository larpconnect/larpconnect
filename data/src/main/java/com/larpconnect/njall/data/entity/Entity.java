package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.Generated;

@jakarta.persistence.Entity
@Table(name = "entities")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class Entity {
  public Entity() {}

  @Id @Generated private UUID id;

  @Column(name = "entity_type", insertable = false, updatable = false)
  private String entityType;

  @Column(name = "external_reference")
  private String externalReference;

  @Column(name = "created_on", insertable = false, updatable = false)
  private OffsetDateTime createdOn;

  @Column(name = "updated_on", insertable = false, updatable = false)
  private OffsetDateTime updatedOn;

  @Column(name = "deleted_on", insertable = false, updatable = false)
  private OffsetDateTime deletedOn;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getEntityType() {
    return entityType;
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

  public OffsetDateTime getUpdatedOn() {
    return updatedOn;
  }

  public OffsetDateTime getDeletedOn() {
    return deletedOn;
  }
}
