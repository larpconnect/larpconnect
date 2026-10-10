package com.larpconnect.njall.data.domain;

import java.util.UUID;

/** Base sealed interface for all persistent domain entities and records in Njall. */
public sealed interface DatabaseObject
    permits Server,
        AdminUser,
        AdminRole,
        StudioLookup,
        Studio,
        DefaultStudioRole,
        Link,
        Location,
        Address,
        Hashtag {

  /**
   * Returns the unique identifier of the persistent database object.
   *
   * @return The UUID identifier.
   */
  UUID id();
}
