import os

base_dir = "data/src/main/java/com/larpconnect/njall/data"

studio_java = """package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Studio entity.
 */
@Entity
@Table(name = "studios")
public final class Studio extends NjallEntity {

    @Column(name = "name", nullable = false)
    private String name;

    public Studio() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
"""

role_java = """package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Role entity.
 */
@Entity
@Table(name = "roles")
public final class Role extends NjallEntity {

    @Column(name = "role_name", nullable = false)
    private String roleName;

    public Role() {
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}
"""

studio_role_id_java = """package com.larpconnect.njall.data;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.Embeddable;

/**
 * Studio role id.
 */
@Embeddable
public final class StudioRoleId implements Serializable {
    private UUID studioId;
    private UUID roleId;
    private UUID individualId;

    public StudioRoleId() {
    }

    public StudioRoleId(UUID studioId, UUID roleId, UUID individualId) {
        this.studioId = studioId;
        this.roleId = roleId;
        this.individualId = individualId;
    }

    public UUID getStudioId() {
        return studioId;
    }

    public void setStudioId(UUID studioId) {
        this.studioId = studioId;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
    }

    public UUID getIndividualId() {
        return individualId;
    }

    public void setIndividualId(UUID individualId) {
        this.individualId = individualId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StudioRoleId that = (StudioRoleId) o;
        return Objects.equals(studioId, that.studioId) && Objects.equals(roleId, that.roleId) && Objects.equals(individualId, that.individualId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studioId, roleId, individualId);
    }
}
"""

studio_role_java = """package com.larpconnect.njall.data;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Studio role entity.
 */
@Entity
@Table(name = "studio_roles")
public final class StudioRole {

    @EmbeddedId
    private StudioRoleId id;

    public StudioRole() {
    }

    public StudioRoleId getId() {
        return id;
    }

    public void setId(StudioRoleId id) {
        this.id = id;
    }
}
"""

location_java = """package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Location entity.
 */
@Entity
@Table(name = "locations")
public final class Location extends NjallEntity {

    @Column(name = "address", nullable = false)
    private String address;

    public Location() {
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
"""

def write_file(filename, content):
    with open(os.path.join(base_dir, filename), "w") as f:
        f.write(content)

write_file("Studio.java", studio_java)
write_file("Role.java", role_java)
write_file("StudioRoleId.java", studio_role_id_java)
write_file("StudioRole.java", studio_role_java)
write_file("Location.java", location_java)

print("Created Studio, Role, StudioRole, and Location.")
