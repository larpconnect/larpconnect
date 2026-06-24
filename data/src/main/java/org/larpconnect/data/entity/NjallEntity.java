package org.larpconnect.data.entity;

import java.util.UUID;

/** Common sealed interface for all LarpConnect entities. */
public sealed interface NjallEntity permits AbstractEntity {
  /** Returns the unique identifier for the entity. */
  UUID getId();
}
