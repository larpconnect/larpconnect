package com.larpconnect.njall.data.dao.studios;

import com.larpconnect.njall.data.dao.common.DAO;
import com.larpconnect.njall.data.domain.Studio;
import java.util.Optional;
import java.util.UUID;

/** Data Access Object for tenanted studio retrieval in user space. */
public interface StudioDAO extends DAO<Studio> {

  /**
   * Retrieves a studio by its internal tenant UUID, enforcing row-level security.
   *
   * @param tenantId The tenant UUID identifier.
   * @return An Optional containing the studio if found within the tenant boundary, otherwise empty.
   */
  @Override
  Optional<Studio> findById(UUID tenantId);

  /**
   * Alias for {@link #findById(UUID)} providing studio information for the given tenant ID.
   *
   * @param tenantId The tenant UUID identifier.
   * @return An Optional containing the studio if found, otherwise empty.
   */
  default Optional<Studio> getStudio(UUID tenantId) {
    return findById(tenantId);
  }
}
