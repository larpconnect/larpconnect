package com.larpconnect.njall.data.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.Generated;

@Entity
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

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public UUID getImplicitId() {
    return implicitId;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setImplicitId(UUID implicitId) {
    this.implicitId = implicitId;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public Collection getCollection() {
    return collection;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setCollection(Collection collection) {
    this.collection = collection;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public OffsetDateTime getAddedOn() {
    return addedOn;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public Entity getRefersTo() {
    return refersTo;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setRefersTo(Entity refersTo) {
    this.refersTo = refersTo;
  }
}
