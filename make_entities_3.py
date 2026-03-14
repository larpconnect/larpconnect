import os

base_dir = "data/src/main/java/com/larpconnect/njall/data"

individual_java = """package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Individual entity.
 */
@Entity
@Table(name = "individuals")
public class Individual extends NjallEntity {

    @Column(name = "name", nullable = false)
    private String name;

    public Individual() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
"""

user_java = """package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * User entity.
 */
@Entity
@Table(name = "users")
public final class User extends Individual {

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "username", nullable = false)
    private String username;

    public User() {
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
"""

def write_file(filename, content):
    with open(os.path.join(base_dir, filename), "w") as f:
        f.write(content)

write_file("Individual.java", individual_java)
write_file("User.java", user_java)

print("Created Individual and User.")
