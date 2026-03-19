package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.Role;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

final class DefaultRoleDao extends AbstractDao<Role, UUID> implements RoleDao {

  @Inject
  DefaultRoleDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    super(sessionFactoryProvider, Role.class);
  }
}
