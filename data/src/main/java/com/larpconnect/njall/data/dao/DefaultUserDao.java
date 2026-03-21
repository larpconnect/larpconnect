package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.User;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

final class DefaultUserDao extends AbstractDao<User, UUID> implements UserDao {

  @Inject
  DefaultUserDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    super(sessionFactoryProvider, User.class);
  }
}
