package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.domain.Address;
import com.larpconnect.njall.data.domain.AddressType;
import com.larpconnect.njall.data.domain.GeoJsonPoint;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

final class DefaultPatchBuilder implements AddressDAO.PatchBuilder {

  private final UUID tenantId;
  private final UUID locationId;
  private final UUID addressId;
  private final Function<DefaultPatchBuilder, Optional<Address>> executor;

  private @Nullable AddressType addressType;
  private @Nullable String addressLine1;
  private @Nullable String addressLine2;
  private @Nullable String addressLine3;
  private @Nullable String locality;
  private @Nullable String administrativeArea;
  private @Nullable String postalCode;
  private @Nullable String countryCode;
  private boolean updateGeom;
  private @Nullable GeoJsonPoint geom;

  DefaultPatchBuilder(
      UUID tenantId,
      UUID locationId,
      UUID addressId,
      Function<DefaultPatchBuilder, Optional<Address>> executor) {
    this.tenantId = tenantId;
    this.locationId = locationId;
    this.addressId = addressId;
    this.executor = executor;
  }

  @Override
  public AddressDAO.PatchBuilder addressType(AddressType addressType) {
    this.addressType = addressType;
    return this;
  }

  @Override
  public AddressDAO.PatchBuilder addressType(Optional<AddressType> addressType) {
    addressType.ifPresent(this::addressType);
    return this;
  }

  @Override
  public AddressDAO.PatchBuilder addressLine1(String addressLine1) {
    this.addressLine1 = addressLine1;
    return this;
  }

  @Override
  public AddressDAO.PatchBuilder addressLine1(Optional<String> addressLine1) {
    addressLine1.ifPresent(this::addressLine1);
    return this;
  }

  @Override
  public AddressDAO.PatchBuilder addressLine2(String addressLine2) {
    this.addressLine2 = addressLine2;
    return this;
  }

  @Override
  public AddressDAO.PatchBuilder addressLine2(Optional<String> addressLine2) {
    addressLine2.ifPresent(this::addressLine2);
    return this;
  }

  @Override
  public AddressDAO.PatchBuilder addressLine3(String addressLine3) {
    this.addressLine3 = addressLine3;
    return this;
  }

  @Override
  public AddressDAO.PatchBuilder addressLine3(Optional<String> addressLine3) {
    addressLine3.ifPresent(this::addressLine3);
    return this;
  }

  @Override
  public AddressDAO.PatchBuilder locality(String locality) {
    this.locality = locality;
    return this;
  }

  @Override
  public AddressDAO.PatchBuilder locality(Optional<String> locality) {
    locality.ifPresent(this::locality);
    return this;
  }

  @Override
  public AddressDAO.PatchBuilder administrativeArea(String administrativeArea) {
    this.administrativeArea = administrativeArea;
    return this;
  }

  @Override
  public AddressDAO.PatchBuilder administrativeArea(Optional<String> administrativeArea) {
    administrativeArea.ifPresent(this::administrativeArea);
    return this;
  }

  @Override
  public AddressDAO.PatchBuilder postalCode(String postalCode) {
    this.postalCode = postalCode;
    return this;
  }

  @Override
  public AddressDAO.PatchBuilder postalCode(Optional<String> postalCode) {
    postalCode.ifPresent(this::postalCode);
    return this;
  }

  @Override
  public AddressDAO.PatchBuilder countryCode(String countryCode) {
    this.countryCode = countryCode;
    return this;
  }

  @Override
  public AddressDAO.PatchBuilder countryCode(Optional<String> countryCode) {
    countryCode.ifPresent(this::countryCode);
    return this;
  }

  @Override
  public AddressDAO.PatchBuilder geom(@Nullable GeoJsonPoint geom) {
    this.updateGeom = true;
    this.geom = geom;
    return this;
  }

  @Override
  public AddressDAO.PatchBuilder geom(Optional<GeoJsonPoint> geom) {
    this.updateGeom = true;
    this.geom = geom.orElse(null);
    return this;
  }

  @Override
  public AddressDAO.PatchBuilder clearGeom() {
    this.updateGeom = true;
    this.geom = null;
    return this;
  }

  @Override
  public Optional<Address> execute() {
    return executor.apply(this);
  }

  UUID tenantId() {
    return tenantId;
  }

  UUID locationId() {
    return locationId;
  }

  UUID addressId() {
    return addressId;
  }

  @Nullable AddressType addressType() {
    return addressType;
  }

  @Nullable String addressLine1() {
    return addressLine1;
  }

  @Nullable String addressLine2() {
    return addressLine2;
  }

  @Nullable String addressLine3() {
    return addressLine3;
  }

  @Nullable String locality() {
    return locality;
  }

  @Nullable String administrativeArea() {
    return administrativeArea;
  }

  @Nullable String postalCode() {
    return postalCode;
  }

  @Nullable String countryCode() {
    return countryCode;
  }

  boolean updateGeom() {
    return updateGeom;
  }

  @Nullable GeoJsonPoint geom() {
    return geom;
  }
}
