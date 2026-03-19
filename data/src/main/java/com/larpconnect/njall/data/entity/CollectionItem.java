package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Items within a collection. */
@jakarta.persistence.Entity
@Table(name = "collection_items")
public final class CollectionItem implements DatabaseObject {

  @Id
  @Column(name = "id")
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "collection_id", nullable = false)
  private Collection collection;

  @Column(name = "added_on", nullable = false, insertable = false, updatable = false)
  private OffsetDateTime addedOn;

  @ManyToOne
  @JoinColumn(name = "refers_to", nullable = false)
  private Entity refersTo;

  CollectionItem() {}

  @Override
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Collection getCollection() {
    return collection;
  }

  public void setCollection(Collection collection) {
    this.collection = collection;
  }

  public OffsetDateTime getAddedOn() {
    return addedOn;
  }

  public void setAddedOn(OffsetDateTime addedOn) {
    this.addedOn = addedOn;
  }

  public Entity getRefersTo() {
    return refersTo;
  }

  public void setRefersTo(Entity refersTo) {
    this.refersTo = refersTo;
  }
}
