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

  AddressEntity(
      UUID tenantId,
      UUID id,
      UUID locationId,
      String addressType,
      String addressLine1,
      String addressLine2,
      String addressLine3,
      String locality,
      String administrativeArea,
      String postalCode,
      String countryCode) {
    this.tenantId = tenantId;
    this.id = id;
    this.locationId = locationId;
    this.addressType = addressType;
    this.addressLine1 = addressLine1;
    this.addressLine2 = addressLine2;
    this.addressLine3 = addressLine3;
    this.locality = locality;
    this.administrativeArea = administrativeArea;
    this.postalCode = postalCode;
    this.countryCode = countryCode;
  }

  UUID getTenantId() {
    return tenantId;
  }

  void setTenantId(UUID tenantId) {
    this.tenantId = tenantId;
  }

  UUID getId() {
    return id;
  }

  void setId(UUID id) {
    this.id = id;
  }

  UUID getLocationId() {
    return locationId;
  }

  void setLocationId(UUID locationId) {
    this.locationId = locationId;
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
