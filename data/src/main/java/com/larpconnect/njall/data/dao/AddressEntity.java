package com.larpconnect.njall.data.dao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

/** JPA entity mapping the {@code njall_users.addresses} subordinate table. */
@Entity
@Table(name = "addresses", schema = "njall_users")
@IdClass(EntityId.class)
class AddressEntity {

  @Id
  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "location_id", nullable = false, updatable = false)
  private UUID locationId;

  @Column(name = "address_type", nullable = false)
  private String addressType;

  @Column(name = "address_line_1", nullable = false)
  private String addressLine1;

  @Column(name = "address_line_2", nullable = false)
  private String addressLine2;

  @Column(name = "address_line_3", nullable = false)
  private String addressLine3;

  @Column(name = "locality", nullable = false)
  private String locality;

  @Column(name = "administrative_area", nullable = false)
  private String administrativeArea;

  @Column(name = "postal_code", nullable = false)
  private String postalCode;

  @Column(name = "country_code", nullable = false)
  private String countryCode;

  AddressEntity() {}

  AddressEntity(UUID tenantId, UUID id, UUID locationId) {
    this.tenantId = tenantId;
    this.id = id;
    this.locationId = locationId;
  }

  static Builder builder(UUID tenantId, UUID id, UUID locationId) {
    return new Builder(tenantId, id, locationId);
  }

  UUID getTenantId() {
    return tenantId;
  }

  UUID getId() {
    return id;
  }

  UUID getLocationId() {
    return locationId;
  }

  static final class Builder {
    private final AddressEntity entity;

    Builder(UUID tenantId, UUID id, UUID locationId) {
      this.entity = new AddressEntity(tenantId, id, locationId);
    }

    Builder addressType(String addressType) {
      entity.setAddressType(addressType);
      return this;
    }

    Builder addressLine1(String addressLine1) {
      entity.setAddressLine1(addressLine1);
      return this;
    }

    Builder addressLine2(String addressLine2) {
      entity.setAddressLine2(addressLine2);
      return this;
    }

    Builder addressLine3(String addressLine3) {
      entity.setAddressLine3(addressLine3);
      return this;
    }

    Builder locality(String locality) {
      entity.setLocality(locality);
      return this;
    }

    Builder administrativeArea(String administrativeArea) {
      entity.setAdministrativeArea(administrativeArea);
      return this;
    }

    Builder postalCode(String postalCode) {
      entity.setPostalCode(postalCode);
      return this;
    }

    Builder countryCode(String countryCode) {
      entity.setCountryCode(countryCode);
      return this;
    }

    AddressEntity build() {
      return entity;
    }
  }

  String getAddressType() {
    return addressType;
  }

  void setAddressType(String addressType) {
    this.addressType = addressType;
  }

  String getAddressLine1() {
    return addressLine1;
  }

  void setAddressLine1(String addressLine1) {
    this.addressLine1 = addressLine1;
  }

  String getAddressLine2() {
    return addressLine2;
  }

  void setAddressLine2(String addressLine2) {
    this.addressLine2 = addressLine2;
  }

  String getAddressLine3() {
    return addressLine3;
  }

  void setAddressLine3(String addressLine3) {
    this.addressLine3 = addressLine3;
  }

  String getLocality() {
    return locality;
  }

  void setLocality(String locality) {
    this.locality = locality;
  }

  String getAdministrativeArea() {
    return administrativeArea;
  }

  void setAdministrativeArea(String administrativeArea) {
    this.administrativeArea = administrativeArea;
  }

  String getPostalCode() {
    return postalCode;
  }

  void setPostalCode(String postalCode) {
    this.postalCode = postalCode;
  }

  String getCountryCode() {
    return countryCode;
  }

  void setCountryCode(String countryCode) {
    this.countryCode = countryCode;
  }
}
