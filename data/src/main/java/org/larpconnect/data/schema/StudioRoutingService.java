package org.larpconnect.data.schema;

import java.util.Optional;
import java.util.UUID;

/** Service for resolving dynamic schema routing based on studio IDs or names. */
public interface StudioRoutingService {
  /**
   * Maps a studio ID to its database schema name.
   *
   * @param studioId The studio ID.
   * @return The schema name, or Optional.empty() if not found.
   */
  Optional<String> getSchemaName(UUID studioId);

  /**
   * Maps a studio name to its database schema name.
   *
   * @param name The studio name.
   * @return The schema name, or Optional.empty() if not found.
   */
  Optional<String> getSchemaName(String name);
}
