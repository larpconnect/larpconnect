import os

base_dir = "data/src/main/java/com/larpconnect/njall/data"
os.makedirs(base_dir, exist_ok=True)

entity_java = """package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Base entity for the Njall data model.
 */
@Entity
@Table(name = "entities")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class NjallEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "external_reference", length = 2048)
    private String externalReference;

    @Column(name = "created_on", nullable = false, updatable = false)
    private OffsetDateTime createdOn = OffsetDateTime.now();

    @Column(name = "updated_on", nullable = false)
    private OffsetDateTime updatedOn = OffsetDateTime.now();

    @Column(name = "deleted_on")
    private OffsetDateTime deletedOn;

    public NjallEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public OffsetDateTime getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(OffsetDateTime createdOn) {
        this.createdOn = createdOn;
    }

    public OffsetDateTime getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(OffsetDateTime updatedOn) {
        this.updatedOn = updatedOn;
    }

    public OffsetDateTime getDeletedOn() {
        return deletedOn;
    }

    public void setDeletedOn(OffsetDateTime deletedOn) {
        this.deletedOn = deletedOn;
    }
}
"""

server_metadata_java = """package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Server metadata entity.
 */
@Entity
@Table(name = "server_metadata")
public final class ServerMetadata extends NjallEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "admin", nullable = false)
    private String admin;

    @Column(name = "security", nullable = false)
    private String security;

    @Column(name = "support", nullable = false)
    private String support;

    public ServerMetadata() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAdmin() {
        return admin;
    }

    public void setAdmin(String admin) {
        this.admin = admin;
    }

    public String getSecurity() {
        return security;
    }

    public void setSecurity(String security) {
        this.security = security;
    }

    public String getSupport() {
        return support;
    }

    public void setSupport(String support) {
        this.support = support;
    }
}
"""

contact_type_java = """package com.larpconnect.njall.data;

/**
 * Contact type enum.
 */
public enum ContactType {
    email,
    phone,
    address
}
"""

contact_info_java = """package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Contact info entity.
 */
@Entity
@Table(name = "contact_info")
public final class ContactInfo {

    @Id
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "ordering", nullable = false)
    private Integer ordering;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_type", nullable = false)
    private ContactType contactType;

    @Column(name = "contact", nullable = false)
    private String contact;

    @Column(name = "active", nullable = false)
    private Boolean active;

    public ContactInfo() {
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public Integer getOrdering() {
        return ordering;
    }

    public void setOrdering(Integer ordering) {
        this.ordering = ordering;
    }

    public ContactType getContactType() {
        return contactType;
    }

    public void setContactType(ContactType contactType) {
        this.contactType = contactType;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
"""

def write_file(filename, content):
    with open(os.path.join(base_dir, filename), "w") as f:
        f.write(content)

write_file("NjallEntity.java", entity_java)
write_file("ServerMetadata.java", server_metadata_java)
write_file("ContactType.java", contact_type_java)
write_file("ContactInfo.java", contact_info_java)

print("Created Entity, ServerMetadata, ContactType, and ContactInfo.")
