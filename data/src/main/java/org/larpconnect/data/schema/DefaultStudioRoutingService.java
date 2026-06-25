package org.larpconnect.data.schema;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Dynamic schema routing service using Caffeine caching. */
final class DefaultStudioRoutingService implements StudioRoutingService {
  private final Logger logger = LoggerFactory.getLogger(DefaultStudioRoutingService.class);

  private final Provider<SessionFactory> sessionFactoryProvider;
  private final LoadingCache<String, StudioMappings> cache;

  private record StudioMappings(Map<UUID, String> idToSchema, Map<String, String> nameToSchema) {}

  @Inject
  DefaultStudioRoutingService(Provider<SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
    this.cache =
        Caffeine.newBuilder().refreshAfterWrite(Duration.ofMinutes(5)).build(key -> loadMappings());
  }

  @Override
  public Optional<String> getSchemaName(UUID studioId) {
    if (studioId == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(cache.get("all").idToSchema().get(studioId));
  }

  @Override
  public Optional<String> getSchemaName(String name) {
    if (name == null) {
      return Optional.empty();
    }
    if ("admin".equalsIgnoreCase(name)) {
      return Optional.of("njall_core_admin");
    }
    if ("njall_core_default".equalsIgnoreCase(name)) {
      return Optional.of("njall_core_default");
    }
    return Optional.ofNullable(cache.get("all").nameToSchema().get(name));
  }

  private StudioMappings loadMappings() {
    try {
      SessionFactory sf = sessionFactoryProvider.get();
      try (Session session =
          sf.withOptions().tenantIdentifier((Object) "njall_core_admin").openSession()) {
        session.beginTransaction();
        List<Object[]> results =
            session
                .createNativeQuery(
                    "SELECT id, name, schema_name FROM studios WHERE deleted_at IS NULL",
                    Object[].class)
                .getResultList();

        Map<UUID, String> idToSchema = new HashMap<>();
        Map<String, String> nameToSchema = new HashMap<>();

        for (Object[] row : results) {
          UUID id = (UUID) row[0];
          String name = (String) row[1];
          String schemaName = (String) row[2];
          idToSchema.put(id, schemaName);
          nameToSchema.put(name, schemaName);
        }

        session.getTransaction().commit();
        return new StudioMappings(idToSchema, nameToSchema);
      }
    } catch (Exception e) {
      logger.error("Failed to load studio routing mappings from database", e);
      throw new RuntimeException("Failed to load studio routing mappings", e);
    }
  }
}
