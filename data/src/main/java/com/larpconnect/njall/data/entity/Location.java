package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "locations")
public class Location extends com.larpconnect.njall.data.entity.Entity {
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
