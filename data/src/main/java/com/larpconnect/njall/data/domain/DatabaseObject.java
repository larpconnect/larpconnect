package com.larpconnect.njall.data.domain;

import java.util.UUID;

/** Base sealed interface for all persistent domain entities and records in Project Njall. */
public sealed interface DatabaseObject permits Server {

  /**
   * Returns the unique identifier of the persistent database object.
   *
   * @return The UUID identifier.
   */
  UUID id();
}
