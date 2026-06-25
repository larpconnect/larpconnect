package org.larpconnect.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import java.util.UUID;

/** Individual entity scoped to a tenant schema. Permits User. */
@Entity
@Table(name = "individuals")
@Inheritance(strategy = InheritanceType.JOINED)
public sealed class Individual extends TenantEntity permits User {
  @Column(name = "name", nullable = false)
  private String name;

  public Individual() {}

  public Individual(UUID id, String name) {
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
