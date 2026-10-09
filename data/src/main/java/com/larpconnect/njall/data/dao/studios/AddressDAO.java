package com.larpconnect.njall.data.dao.studios;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.dao.common.DAO;
import com.larpconnect.njall.data.domain.Address;
import com.larpconnect.njall.data.domain.AddressType;
import com.larpconnect.njall.data.domain.GeoJsonPoint;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Data Access Object for tenanted subordinate address operations in user space. */
public interface AddressDAO extends DAO<Address> {

  /**
   * Retrieves an address by studio tenant UUID, parent location UUID, and address UUID.
   *
   * @param tenantId The internal tenant UUID.
   * @param locationId The parent location UUID.
   * @param addressId The address UUID identifier.
   * @return An Optional containing the address if found and parent location is active, otherwise
   *     empty.
   */
  Optional<Address> findById(UUID tenantId, UUID locationId, UUID addressId);

  /**
   * Lists all addresses associated with an active location under the specified tenant.
   *
   * @param tenantId The internal tenant UUID.
   * @param locationId The parent location UUID.
   * @return An immutable list of addresses.
   */
  ImmutableList<Address> listByLocation(UUID tenantId, UUID locationId);

  /**
   * Begins a fluid build chain to persist a new subordinate address.
   *
   * @param tenantId The internal tenant UUID.
   * @param locationId The parent location UUID.
   * @return A fluid CreateBuilder instance.
   */
  CreateBuilder create(UUID tenantId, UUID locationId);

  /**
   * Begins a fluid build chain to apply partial updates to an existing address.
   *
   * @param tenantId The internal tenant UUID.
   * @param locationId The parent location UUID.
   * @param addressId The address UUID identifier.
   * @return A fluid PatchBuilder instance.
   */
  PatchBuilder patch(UUID tenantId, UUID locationId, UUID addressId);

  /**
   * Physically removes an address from the database.
   *
   * @param tenantId The internal tenant UUID.
   * @param locationId The parent location UUID.
   * @param addressId The address UUID identifier.
   * @return true if the address was found and removed, false otherwise.
   */
  boolean delete(UUID tenantId, UUID locationId, UUID addressId);

  @Override
  default Optional<Address> findById(UUID id) {
    throw new UnsupportedOperationException(
        """
        Direct address lookup requires tenant and location context; use findById(tenantId, \
        locationId, addressId)\
        """);
  }

  /** Fluid builder for persisting a new subordinate address. */
  interface CreateBuilder {

    CreateBuilder addressType(AddressType addressType);

    CreateBuilder addressLine1(String addressLine1);

    CreateBuilder addressLine2(String addressLine2);

    CreateBuilder addressLine2(Optional<String> addressLine2);

    CreateBuilder addressLine3(String addressLine3);

    CreateBuilder addressLine3(Optional<String> addressLine3);

    CreateBuilder locality(String locality);

    CreateBuilder administrativeArea(String administrativeArea);

    CreateBuilder postalCode(String postalCode);

    CreateBuilder countryCode(String countryCode);

    CreateBuilder geom(@Nullable GeoJsonPoint geom);

    CreateBuilder geom(Optional<GeoJsonPoint> geom);

    Address execute();
  }

  /** Fluid builder for applying partial updates to an existing address (AIP-134). */
  interface PatchBuilder {

    PatchBuilder addressType(AddressType addressType);

    PatchBuilder addressType(Optional<AddressType> addressType);

    PatchBuilder addressLine1(String addressLine1);

    PatchBuilder addressLine1(Optional<String> addressLine1);

    PatchBuilder addressLine2(String addressLine2);

    PatchBuilder addressLine2(Optional<String> addressLine2);

    PatchBuilder addressLine3(String addressLine3);

    PatchBuilder addressLine3(Optional<String> addressLine3);

    PatchBuilder locality(String locality);

    PatchBuilder locality(Optional<String> locality);

    PatchBuilder administrativeArea(String administrativeArea);

    PatchBuilder administrativeArea(Optional<String> administrativeArea);

    PatchBuilder postalCode(String postalCode);

    PatchBuilder postalCode(Optional<String> postalCode);

    PatchBuilder countryCode(String countryCode);

    PatchBuilder countryCode(Optional<String> countryCode);

    PatchBuilder geom(@Nullable GeoJsonPoint geom);

    PatchBuilder geom(Optional<GeoJsonPoint> geom);

    PatchBuilder clearGeom();

    Optional<Address> execute();
  }
}
