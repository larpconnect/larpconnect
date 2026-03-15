package com.larpconnect.njall.data.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "studios")
public class Studio extends com.larpconnect.njall.data.entity.Entity {
  public Studio() {}

  @Column(name = "name")
  private String name;

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public String getName() {
    return name;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setName(String name) {
    this.name = name;
  }
}
