package com.larpconnect.njall.data.dao;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Doubles;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.larpconnect.njall.data.annotation.NjallUsers;
import com.larpconnect.njall.data.domain.Address;
import com.larpconnect.njall.data.domain.AddressType;
import com.larpconnect.njall.data.domain.GeoJsonPoint;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jspecify.annotations.Nullable;

final class DefaultAddressDAO implements AddressDAO {

  private static final Pattern COORD_PATTERN =
      Pattern.compile(
          "\\[\\s*(-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)\\s*,"
              + "\\s*(-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)\\s*\\]");

  private static final String SET_TENANT_CONFIG =
      "SELECT set_config('app.tenant_id', :tenantId, true)";

  private static final String SQL_FIND_BY_ID =
      "SELECT a.id, a.location_id, a.address_type, a.address_line_1, a.address_line_2,"
          + " a.address_line_3, a.locality, a.administrative_area, a.postal_code,"
          + " a.country_code, ST_AsGeoJSON(a.geom) "
          + "FROM njall_users.addresses a "
          + "JOIN njall_users.entities e ON e.tenant_id = a.tenant_id AND e.id = a.location_id "
          + "WHERE a.tenant_id = :tenantId AND a.location_id = :locationId AND a.id = :addressId "
          + "  AND e.deleted_on IS NULL";

  private static final String SQL_LIST_BY_LOCATION =
      "SELECT a.id, a.location_id, a.address_type, a.address_line_1, a.address_line_2,"
          + " a.address_line_3, a.locality, a.administrative_area, a.postal_code,"
          + " a.country_code, ST_AsGeoJSON(a.geom) "
          + "FROM njall_users.addresses a "
          + "JOIN njall_users.entities e ON e.tenant_id = a.tenant_id AND e.id = a.location_id "
          + "WHERE a.tenant_id = :tenantId AND a.location_id = :locationId "
          + "  AND e.deleted_on IS NULL "
          + "ORDER BY a.id ASC";

  private static final String SQL_CHECK_LOCATION_ACTIVE =
      "SELECT count(1) FROM njall_users.locations l "
          + "JOIN njall_users.entities e ON e.tenant_id = l.tenant_id AND e.id = l.id "
          + "WHERE l.tenant_id = :tenantId AND l.id = :locationId AND e.deleted_on IS NULL";

  private static final String SQL_UPDATE_GEOM =
      "UPDATE njall_users.addresses SET geom ="
          + " ST_SetSRID(ST_GeomFromGeoJSON(cast(:geomJson as text)), 4326)::geography "
          + "WHERE tenant_id = :tenantId AND id = :addressId";

  private static final String SQL_CLEAR_GEOM =
      "UPDATE njall_users.addresses SET geom = NULL "
          + "WHERE tenant_id = :tenantId AND id = :addressId";

  private static final String SQL_LOAD_GEOM =
      "SELECT ST_AsGeoJSON(geom) FROM njall_users.addresses "
          + "WHERE tenant_id = :tenantId AND id = :addressId";

  private final Provider<SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultAddressDAO(@NjallUsers Provider<SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Optional<Address> findById(UUID tenantId, UUID locationId, UUID addressId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        List<?> rows =
            session
                .createNativeQuery(SQL_FIND_BY_ID, Object[].class)
                .setParameter("tenantId", tenantId)
                .setParameter("locationId", locationId)
                .setParameter("addressId", addressId)
                .getResultList();
        tx.commit();
        if (rows.isEmpty()) {
          return Optional.empty();
        }
        return Optional.of(toAddress((Object[]) rows.getFirst()));
      } catch (Exception e) {
        try {
          tx.rollback();
        } catch (Exception rollbackException) {
          e.addSuppressed(rollbackException);
        }
        throw e;
      }
    }
  }

  @Override
  public ImmutableList<Address> listByLocation(UUID tenantId, UUID locationId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        List<?> rows =
            session
                .createNativeQuery(SQL_LIST_BY_LOCATION, Object[].class)
                .setParameter("tenantId", tenantId)
                .setParameter("locationId", locationId)
                .getResultList();
        tx.commit();
        var builder = ImmutableList.<Address>builder();
        for (var row : rows) {
          builder.add(toAddress((Object[]) row));
        }
        return builder.build();
      } catch (Exception e) {
        try {
          tx.rollback();
        } catch (Exception rollbackException) {
          e.addSuppressed(rollbackException);
        }
        throw e;
      }
    }
  }

  @Override
  public CreateBuilder create(UUID tenantId, UUID locationId) {
    return new DefaultCreateBuilder(tenantId, locationId, this::executeCreate);
  }

  @Override
  public PatchBuilder patch(UUID tenantId, UUID locationId, UUID addressId) {
    return new DefaultPatchBuilder(tenantId, locationId, addressId, this::executePatch);
  }

  @Override
  public boolean delete(UUID tenantId, UUID locationId, UUID addressId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = new EntityId(tenantId, addressId);
        var entity = session.find(AddressEntity.class, entityId);
        if (entity == null || !entity.getLocationId().equals(locationId)) {
          tx.commit();
          return false;
        }
        session.remove(entity);
        tx.commit();
        return true;
      } catch (Exception e) {
        try {
          tx.rollback();
        } catch (Exception rollbackException) {
          e.addSuppressed(rollbackException);
        }
        throw e;
      }
    }
  }

  @Override
  public ImmutableList<Address> list() {
    throw new UnsupportedOperationException(
        "Direct address listing requires location context; use listByLocation(tenantId,"
            + " locationId)");
  }

  private void setTenantContext(Session session, UUID tenantId) {
    session
        .createNativeQuery(SET_TENANT_CONFIG, String.class)
        .setParameter("tenantId", tenantId.toString())
        .getSingleResult();
  }

  private static boolean isLocationActive(Session session, UUID tenantId, UUID locationId) {
    var count =
        session
            .createNativeQuery(SQL_CHECK_LOCATION_ACTIVE, Long.class)
            .setParameter("tenantId", tenantId)
            .setParameter("locationId", locationId)
            .getSingleResult();
    return count != null && count > 0;
  }

  private Address executeCreate(DefaultCreateBuilder b) {
    requireNonNull(b.addressType(), "addressType must be specified");
    requireNonNull(b.addressLine1(), "addressLine1 must be specified");
    requireNonNull(b.locality(), "locality must be specified");
    requireNonNull(b.administrativeArea(), "administrativeArea must be specified");
    requireNonNull(b.postalCode(), "postalCode must be specified");
    requireNonNull(b.countryCode(), "countryCode must be specified");

    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, b.tenantId());
        if (!isLocationActive(session, b.tenantId(), b.locationId())) {
          throw new IllegalArgumentException(
              "Parent location not found or inactive for tenant: " + b.tenantId());
        }
        var addressId = UUID.randomUUID();
        var entity =
            AddressEntity.builder(b.tenantId(), addressId, b.locationId())
                .addressType(b.addressType().name())
                .addressLine1(b.addressLine1())
                .addressLine2(b.addressLine2())
                .addressLine3(b.addressLine3())
                .locality(b.locality())
                .administrativeArea(b.administrativeArea())
                .postalCode(b.postalCode())
                .countryCode(b.countryCode())
                .build();
        session.persist(entity);

        if (b.geom() != null) {
          session.flush();
          updateGeom(session, b.tenantId(), addressId, b.geom());
        }
        tx.commit();
        return toAddress(entity, b.geom());
      } catch (Exception e) {
        try {
          tx.rollback();
        } catch (Exception rollbackException) {
          e.addSuppressed(rollbackException);
        }
        throw e;
      }
    }
  }

  private Optional<Address> executePatch(DefaultPatchBuilder b) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, b.tenantId());
        if (!isLocationActive(session, b.tenantId(), b.locationId())) {
          tx.commit();
          return Optional.empty();
        }
        var entityId = new EntityId(b.tenantId(), b.addressId());
        var entity = session.find(AddressEntity.class, entityId);
        if (entity == null || !entity.getLocationId().equals(b.locationId())) {
          tx.commit();
          return Optional.empty();
        }
        applyPatchFields(entity, b);
        session.merge(entity);

        session.flush();
        var effectiveGeom = applyGeomPatch(session, b);
        tx.commit();
        return Optional.of(toAddress(entity, effectiveGeom));
      } catch (Exception e) {
        try {
          tx.rollback();
        } catch (Exception rollbackException) {
          e.addSuppressed(rollbackException);
        }
        throw e;
      }
    }
  }

  private static @Nullable GeoJsonPoint applyGeomPatch(Session session, DefaultPatchBuilder b) {
    if (b.updateGeom()) {
      if (b.geom() != null) {
        updateGeom(session, b.tenantId(), b.addressId(), b.geom());
        return b.geom();
      }
      clearGeom(session, b.tenantId(), b.addressId());
      return null;
    }
    return loadGeom(session, b.tenantId(), b.addressId());
  }

  private static void applyPatchFields(AddressEntity entity, DefaultPatchBuilder b) {
    if (b.addressType() != null) {
      entity.setAddressType(b.addressType().name());
    }
    if (b.addressLine1() != null) {
      entity.setAddressLine1(b.addressLine1());
    }
    if (b.addressLine2() != null) {
      entity.setAddressLine2(b.addressLine2());
    }
    if (b.addressLine3() != null) {
      entity.setAddressLine3(b.addressLine3());
    }
    applyRemainingPatchFields(entity, b);
  }

  private static void applyRemainingPatchFields(AddressEntity entity, DefaultPatchBuilder b) {
    if (b.locality() != null) {
      entity.setLocality(b.locality());
    }
    if (b.administrativeArea() != null) {
      entity.setAdministrativeArea(b.administrativeArea());
    }
    if (b.postalCode() != null) {
      entity.setPostalCode(b.postalCode());
    }
    if (b.countryCode() != null) {
      entity.setCountryCode(b.countryCode());
    }
  }

  private static void updateGeom(
      Session session, UUID tenantId, UUID addressId, GeoJsonPoint point) {
    session
        .createNativeQuery(SQL_UPDATE_GEOM, Void.class)
        .setParameter("tenantId", tenantId)
        .setParameter("addressId", addressId)
        .setParameter("geomJson", toGeoJsonString(point))
        .executeUpdate();
  }

  private static void clearGeom(Session session, UUID tenantId, UUID addressId) {
    session
        .createNativeQuery(SQL_CLEAR_GEOM, Void.class)
        .setParameter("tenantId", tenantId)
        .setParameter("addressId", addressId)
        .executeUpdate();
  }

  private static @Nullable GeoJsonPoint loadGeom(Session session, UUID tenantId, UUID addressId) {
    List<?> rows =
        session
            .createNativeQuery(SQL_LOAD_GEOM, String.class)
            .setParameter("tenantId", tenantId)
            .setParameter("addressId", addressId)
            .getResultList();
    if (rows.isEmpty() || rows.getFirst() == null) {
      return null;
    }
    return parseGeoJson((String) rows.getFirst()).orElse(null);
  }

  private static Address toAddress(AddressEntity entity, @Nullable GeoJsonPoint geom) {
    return new Address(
        entity.getId(),
        entity.getLocationId(),
        AddressType.valueOf(entity.getAddressType()),
        entity.getAddressLine1(),
        entity.getAddressLine2(),
        entity.getAddressLine3(),
        entity.getLocality(),
        entity.getAdministrativeArea(),
        entity.getPostalCode(),
        entity.getCountryCode(),
        geom);
  }

  private static String toGeoJsonString(GeoJsonPoint point) {
    return "{\"type\":\"Point\",\"coordinates\":["
        + point.longitude()
        + ","
        + point.latitude()
        + "]}";
  }

  private static Optional<GeoJsonPoint> parseGeoJson(@Nullable String geoJson) {
    if (geoJson == null || geoJson.isBlank()) {
      return Optional.empty();
    }
    var matcher = COORD_PATTERN.matcher(geoJson);
    if (matcher.find()) {
      var lon = Doubles.tryParse(matcher.group(1));
      var lat = Doubles.tryParse(matcher.group(2));
      if (lon != null && lat != null) {
        return Optional.of(new GeoJsonPoint(lon, lat));
      }
    }
    return Optional.empty();
  }

  private static Address toAddress(Object[] row) {
    var id = (UUID) row[0];
    var locationId = (UUID) row[1];
    var addressType = AddressType.valueOf((String) row[2]);
    var line1 = (String) row[3];
    var line2 = (String) row[4];
    var line3 = (String) row[5];
    var locality = (String) row[6];
    var adminArea = (String) row[7];
    var postalCode = (String) row[8];
    var countryCode = (String) row[9];
    var geomJson = (String) row[10];
    return new Address(
        id,
        locationId,
        addressType,
        line1,
        line2,
        line3,
        locality,
        adminArea,
        postalCode,
        countryCode,
        parseGeoJson(geomJson).orElse(null));
  }
}
