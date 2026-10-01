package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.domain.Address;
import com.larpconnect.njall.data.domain.AddressType;
import com.larpconnect.njall.data.domain.GeoJsonPoint;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

final class DefaultCreateBuilder implements AddressDAO.CreateBuilder {

  private final UUID tenantId;
  private final UUID locationId;
  private final Function<DefaultCreateBuilder, Address> executor;

  private AddressType addressType;
  private String addressLine1;
  private String addressLine2 = "";
  private String addressLine3 = "";
  private String locality;
  private String administrativeArea;
  private String postalCode;
  private String countryCode;
  private @Nullable GeoJsonPoint geom;

  DefaultCreateBuilder(
      UUID tenantId, UUID locationId, Function<DefaultCreateBuilder, Address> executor) {
    this.tenantId = tenantId;
    this.locationId = locationId;
    this.executor = executor;
  }

  @Override
  public AddressDAO.CreateBuilder addressType(AddressType addressType) {
    this.addressType = addressType;
    return this;
  }

  @Override
  public AddressDAO.CreateBuilder addressLine1(String addressLine1) {
    this.addressLine1 = addressLine1;
    return this;
  }

  @Override
  public AddressDAO.CreateBuilder addressLine2(String addressLine2) {
    this.addressLine2 = addressLine2;
    return this;
  }

  @Override
  public AddressDAO.CreateBuilder addressLine2(Optional<String> addressLine2) {
    this.addressLine2 = addressLine2.orElse("");
    return this;
  }

  @Override
  public AddressDAO.CreateBuilder addressLine3(String addressLine3) {
    this.addressLine3 = addressLine3;
    return this;
  }

  @Override
  public AddressDAO.CreateBuilder addressLine3(Optional<String> addressLine3) {
    this.addressLine3 = addressLine3.orElse("");
    return this;
  }

  @Override
  public AddressDAO.CreateBuilder locality(String locality) {
    this.locality = locality;
    return this;
  }

  @Override
  public AddressDAO.CreateBuilder administrativeArea(String administrativeArea) {
    this.administrativeArea = administrativeArea;
    return this;
  }

  @Override
  public AddressDAO.CreateBuilder postalCode(String postalCode) {
    this.postalCode = postalCode;
    return this;
  }

  @Override
  public AddressDAO.CreateBuilder countryCode(String countryCode) {
    this.countryCode = countryCode;
    return this;
  }

  @Override
  public AddressDAO.CreateBuilder geom(@Nullable GeoJsonPoint geom) {
    this.geom = geom;
    return this;
  }

  @Override
  public AddressDAO.CreateBuilder geom(Optional<GeoJsonPoint> geom) {
    this.geom = geom.orElse(null);
    return this;
  }

  @Override
  public Address execute() {
    return executor.apply(this);
  }

  UUID tenantId() {
    return tenantId;
  }

  UUID locationId() {
    return locationId;
  }

  AddressType addressType() {
    return addressType;
  }

  String addressLine1() {
    return addressLine1;
  }

  String addressLine2() {
    return addressLine2;
  }

  String addressLine3() {
    return addressLine3;
  }

  String locality() {
    return locality;
  }

  String administrativeArea() {
    return administrativeArea;
  }

  String postalCode() {
    return postalCode;
  }

  String countryCode() {
    return countryCode;
  }

  @Nullable GeoJsonPoint geom() {
    return geom;
  }
}
