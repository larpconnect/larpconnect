package org.larpconnect.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/** Campaign entity scoped to a tenant schema. */
@Entity
@Table(name = "campaigns")
public final class Campaign extends TenantEntity {
  @ManyToOne
  @JoinColumn(name = "system_id")
  private LarpSystem system;

  public Campaign() {}

  public Campaign(UUID id, LarpSystem system) {
    super(id);
    this.system = system;
  }

  public LarpSystem getSystem() {
    return system;
  }

  public void setSystem(LarpSystem system) {
    this.system = system;
  }
}
