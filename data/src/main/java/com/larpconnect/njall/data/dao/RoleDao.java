package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.Role;
import io.smallrye.mutiny.Uni;
import java.util.UUID;

@DefaultImplementation(DefaultRoleDao.class)
public interface RoleDao {
  Uni<Role> findById(UUID id);

  Uni<Role> persist(Role entity);
}
