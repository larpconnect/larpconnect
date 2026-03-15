package com.larpconnect.njall.data.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "locations")
public class Location extends com.larpconnect.njall.data.entity.Entity {
  public Location() {}

  @Column(name = "address")
  private String address;

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public String getAddress() {
    return address;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setAddress(String address) {
    this.address = address;
  }
}
