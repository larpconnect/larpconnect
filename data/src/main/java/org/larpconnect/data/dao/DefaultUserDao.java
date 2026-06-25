package org.larpconnect.data.dao;

import com.google.inject.Inject;
import org.hibernate.SessionFactory;
import org.larpconnect.data.entity.User;

/** Default implementation of UserDao. */
final class DefaultUserDao extends AbstractDao<User, UserDao> implements UserDao {
  @Inject
  DefaultUserDao(SessionFactory sessionFactory) {
    super(sessionFactory, User.class);
  }
}
