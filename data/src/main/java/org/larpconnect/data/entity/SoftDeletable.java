package org.larpconnect.data.entity;

import java.time.Instant;

/** Interface for entities that support soft deletion. */
public interface SoftDeletable {
  /**
   * Returns the timestamp when the entity was soft-deleted, or null if active.
   *
   * @return The deletion timestamp or null.
   */
  Instant getDeletedTime();

  /**
   * Sets the soft-deletion timestamp.
   *
   * @param deletedTime The deletion timestamp or null to restore.
   */
  void setDeletedTime(Instant deletedTime);
}
