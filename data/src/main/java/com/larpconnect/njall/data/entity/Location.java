package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

/** Represents a location. */
@jakarta.persistence.Entity
@Table(name = "locations")
public final class Location extends Entity {

  @Column(name = "address", nullable = false)
  private String address;

  Location() {}

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }
}
