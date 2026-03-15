package com.larpconnect.njall.data.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public LarpSystem getSystem() {
    return system;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setSystem(LarpSystem system) {
    this.system = system;
  }
}
