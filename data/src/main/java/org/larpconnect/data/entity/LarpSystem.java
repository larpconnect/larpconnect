package org.larpconnect.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/** System entity scoped to a tenant schema. */
@Entity
@Table(name = "systems")
public final class LarpSystem extends TenantEntity {
  @Column(name = "name", nullable = false)
  private String name;

  public LarpSystem() {}

  public LarpSystem(UUID id, String name) {
    super(id);
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
