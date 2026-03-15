package com.larpconnect.njall.data.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public UUID getId() {
    return id;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setId(UUID id) {
    this.id = id;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public String getEntityType() {
    return entityType;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public String getExternalReference() {
    return externalReference;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setExternalReference(String externalReference) {
    this.externalReference = externalReference;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public OffsetDateTime getCreatedOn() {
    return createdOn;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public OffsetDateTime getUpdatedOn() {
    return updatedOn;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public OffsetDateTime getDeletedOn() {
    return deletedOn;
  }
}
