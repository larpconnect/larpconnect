package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.Generated;

@jakarta.persistence.Entity
@Table(name = "collection_items")
public class CollectionItem {
  public CollectionItem() {}

  // Implicitly needed since hibernate requires @Id for entities even without PK in SQL
  @Id @Generated private UUID implicitId;

  @ManyToOne
  @JoinColumn(name = "collection_id")
  private Collection collection;

  @Column(name = "added_on", insertable = false, updatable = false)
  private OffsetDateTime addedOn;

  @ManyToOne
  @JoinColumn(name = "refers_to")
  private Entity refersTo;

  public UUID getImplicitId() {
    return implicitId;
  }

  public void setImplicitId(UUID implicitId) {
    this.implicitId = implicitId;
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

  public Entity getRefersTo() {
    return refersTo;
  }

  public void setRefersTo(Entity refersTo) {
    this.refersTo = refersTo;
  }
}
