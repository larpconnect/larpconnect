package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.Role;
import java.util.UUID;

@DefaultImplementation(DefaultRoleDao.class)
public interface RoleDao extends Dao<Role, UUID> {}
