package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.Role;
import java.util.UUID;

/** DAO for Role. */
@DefaultImplementation(DefaultRoleDao.class)
public interface RoleDao extends Dao<Role, UUID> {}
