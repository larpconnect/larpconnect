package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Location entity. */
@Entity
@Table(name = "locations")
public final class Location extends NjallEntity {

  @Column(name = "address", nullable = false)
  private String address;

  public Location() {}

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }
}
