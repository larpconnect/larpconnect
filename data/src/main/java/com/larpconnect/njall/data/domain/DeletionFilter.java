package com.larpconnect.njall.data.domain;

/** Strategy filter specifying whether queries should include soft-deleted records. */
public enum DeletionFilter {
  /** Include only active, non-deleted records. */
  ACTIVE_ONLY,

  /** Include all records, including soft-deleted entities. */
  INCLUDE_DELETED;

  /**
   * Returns {@code true} if soft-deleted records should be included in query results.
   *
   * @return {@code true} if soft-deleted records are included.
   */
  public boolean includesDeleted() {
    return this == INCLUDE_DELETED;
  }
}
