package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Table;

@jakarta.persistence.Entity
@Table(name = "locations")
@DiscriminatorValue("locations")
public class Location extends Entity {
  public Location() {}

  @Column(name = "address")
  private String address;

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }
}
