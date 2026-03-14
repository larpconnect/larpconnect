import os

base_dir = "data/src/main/java/com/larpconnect/njall/data"

actor_java = """package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Actor entity.
 */
@Entity
@Table(name = "actors")
public final class Actor extends NjallEntity {

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "summary")
    private String summary;

    @Column(name = "preferred_username", nullable = false)
    private String preferredUsername;

    public Actor() {
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getPreferredUsername() {
        return preferredUsername;
    }

    public void setPreferredUsername(String preferredUsername) {
        this.preferredUsername = preferredUsername;
    }
}
"""

actor_endpoint_id_java = """package com.larpconnect.njall.data;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.Embeddable;

/**
 * Actor endpoint id.
 */
@Embeddable
public final class ActorEndpointId implements Serializable {
    private UUID actorId;
    private String name;

    public ActorEndpointId() {
    }

    public ActorEndpointId(UUID actorId, String name) {
        this.actorId = actorId;
        this.name = name;
    }

    public UUID getActorId() {
        return actorId;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActorEndpointId that = (ActorEndpointId) o;
        return Objects.equals(actorId, that.actorId) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actorId, name);
    }
}
"""

actor_endpoint_java = """package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Actor endpoint entity.
 */
@Entity
@Table(name = "actor_endpoints")
public final class ActorEndpoint {

    @EmbeddedId
    private ActorEndpointId id;

    @Column(name = "endpoint", nullable = false)
    private String endpoint;

    public ActorEndpoint() {
    }

    public ActorEndpointId getId() {
        return id;
    }

    public void setId(ActorEndpointId id) {
        this.id = id;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
"""

collection_java = """package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Collection entity.
 */
@Entity
@Table(name = "collections")
public final class Collection extends NjallEntity {

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "name", nullable = false)
    private String name;

    public Collection() {
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
"""

collection_item_java = """package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Collection item entity.
 */
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
    private OffsetDateTime addedOn = OffsetDateTime.now();

    public CollectionItem() {
    }

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
"""

def write_file(filename, content):
    with open(os.path.join(base_dir, filename), "w") as f:
        f.write(content)

write_file("Actor.java", actor_java)
write_file("ActorEndpointId.java", actor_endpoint_id_java)
write_file("ActorEndpoint.java", actor_endpoint_java)
write_file("Collection.java", collection_java)
write_file("CollectionItem.java", collection_item_java)

print("Created Actor, ActorEndpoint, Collection, and CollectionItem.")
