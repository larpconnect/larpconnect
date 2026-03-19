package com.larpconnect.njall.data.entity;

import java.util.UUID;

/** Base sealed interface for all database entities. */
public sealed interface DatabaseObject
    permits ExternalResource, Entity, ContactInfo, SecondaryType, CollectionItem {
  UUID getId();
}
