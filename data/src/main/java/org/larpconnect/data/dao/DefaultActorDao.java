package org.larpconnect.data.dao;

import com.google.inject.Inject;
import org.hibernate.SessionFactory;
import org.larpconnect.data.entity.Actor;

/** Default implementation of ActorDao. */
final class DefaultActorDao extends AbstractDao<Actor, ActorDao> implements ActorDao {
  @Inject
  DefaultActorDao(SessionFactory sessionFactory) {
    super(sessionFactory, Actor.class);
  }
}
