package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.Role;
import java.util.UUID;

/**
 * Provides data access operations for {@link Role} entities.
 *
 * <p>This interface abstracts the persistence mechanism, allowing the application to interact with
 * Role data asynchronously and consistently, regardless of the underlying database schema or ORM
 * framework implementation.
 */
@DefaultImplementation(DefaultRoleDao.class)
public interface RoleDao extends Dao<Role, UUID> {}
