package com.larpconnect.njall.data.domain.common;

import java.util.UUID;

/** Base interface for all persistent domain entities and records in Njall. */
public interface DatabaseObject {
  UUID id();
}
