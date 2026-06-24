package org.larpconnect.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.util.Objects;
import java.util.UUID;

/** Base mapped superclass implementing common entity concerns. */
@MappedSuperclass
public abstract sealed class AbstractEntity implements NjallEntity permits Studio, TenantEntity {
  @Id
  @Column(name = "id")
  private UUID id;

  protected AbstractEntity() {}

  protected AbstractEntity(UUID id) {
    this.id = id;
  }

  @Override
  public final UUID getId() {
    return id;
  }

  public final void setId(UUID id) {
    this.id = id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AbstractEntity)) {
      return false;
    }
    AbstractEntity that = (AbstractEntity) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
