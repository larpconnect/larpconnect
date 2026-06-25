package org.larpconnect.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/** Collection entity scoped to a tenant schema. */
@Entity
@Table(name = "collections")
public final class Collection extends TenantEntity {
  @ManyToOne
  @JoinColumn(name = "owner_id", nullable = false)
  private TenantEntity owner;

  @Column(name = "name", nullable = false)
  private String name;

  public Collection() {}

  public Collection(UUID id, TenantEntity owner, String name) {
    super(id);
    this.owner = owner;
    this.name = name;
  }

  public TenantEntity getOwner() {
    return owner;
  }

  public void setOwner(TenantEntity owner) {
    this.owner = owner;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
