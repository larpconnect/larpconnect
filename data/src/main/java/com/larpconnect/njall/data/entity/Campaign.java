package com.larpconnect.njall.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "campaigns")
public class Campaign extends com.larpconnect.njall.data.entity.Entity {
  public Campaign() {}

  @ManyToOne
  @JoinColumn(name = "system_id")
  private LarpSystem system;

  public LarpSystem getSystem() {
    return system;
  }

  public void setSystem(LarpSystem system) {
    this.system = system;
  }
}
