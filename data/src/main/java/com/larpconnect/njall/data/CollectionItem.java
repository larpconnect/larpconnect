package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Collection item entity. */
@Entity
@Table(name = "collection_items")
public final class CollectionItem {

  // Using refersTo as the ID for simpler mapping, as it's a join table.
  // In practice, this table has no primary key in the script, so we use refersTo.
  @Id
  @Column(name = "refers_to", nullable = false)
  private UUID refersTo;

  @Column(name = "collection_id")
  private UUID collectionId;

  @Column(name = "added_on", nullable = false)
  private OffsetDateTime addedOn = OffsetDateTime.now(java.time.ZoneOffset.UTC);

  public CollectionItem() {}

  public UUID getCollectionId() {
    return collectionId;
  }

  public void setCollectionId(UUID collectionId) {
    this.collectionId = collectionId;
  }

  public OffsetDateTime getAddedOn() {
    return addedOn;
  }

  public void setAddedOn(OffsetDateTime addedOn) {
    this.addedOn = addedOn;
  }

  public UUID getRefersTo() {
    return refersTo;
  }

  public void setRefersTo(UUID refersTo) {
    this.refersTo = refersTo;
  }
}
