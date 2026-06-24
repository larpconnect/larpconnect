package org.larpconnect.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Generated;

/** Polymorphic base entity for tenant-isolated objects using Joined Inheritance. */
@Entity
@Table(name = "entities")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "entity_type")
public abstract sealed class TenantEntity extends AbstractEntity
    permits Campaign,
        LarpSystem,
        Game,
        LarpCharacter,
        CharacterInstance,
        Individual,
        Actor,
        Collection {

  @Column(name = "created_on", insertable = false, updatable = false)
  @Generated
  private Instant createdOn;

  @Column(name = "updated_on", insertable = false, updatable = false)
  @Generated
  private Instant updatedOn;

  @Column(name = "deleted_on")
  private Instant deletedOn;

  protected TenantEntity() {}

  protected TenantEntity(UUID id) {
    super(id);
  }

  public final Instant getCreatedOn() {
    return createdOn;
  }

  public final Instant getUpdatedOn() {
    return updatedOn;
  }

  public final Instant getDeletedOn() {
    return deletedOn;
  }

  public final void setDeletedOn(Instant deletedOn) {
    this.deletedOn = deletedOn;
  }
}
