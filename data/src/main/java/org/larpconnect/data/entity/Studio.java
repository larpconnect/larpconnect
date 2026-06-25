package org.larpconnect.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Generated;

/** Studio entity mapping the admin studios table. */
@Entity
@Table(name = "studios")
public final class Studio extends AbstractEntity implements SoftDeletable {
  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "schema_name", nullable = false)
  private String schemaName;

  @Column(name = "created_at", insertable = false, updatable = false)
  @Generated
  private Instant createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  @Generated
  private Instant updatedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  public Studio() {}

  public Studio(UUID id, String name, String schemaName) {
    super(id);
    this.name = name;
    this.schemaName = schemaName;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSchemaName() {
    return schemaName;
  }

  public void setSchemaName(String schemaName) {
    this.schemaName = schemaName;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }

  public void setDeletedAt(Instant deletedAt) {
    this.deletedAt = deletedAt;
  }

  @Override
  public Instant getDeletedTime() {
    return getDeletedAt();
  }

  @Override
  public void setDeletedTime(Instant deletedTime) {
    setDeletedAt(deletedTime);
  }
}
