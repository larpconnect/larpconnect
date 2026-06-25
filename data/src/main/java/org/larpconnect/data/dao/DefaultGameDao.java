package org.larpconnect.data.dao;

import com.google.inject.Inject;
import org.hibernate.SessionFactory;
import org.larpconnect.data.entity.Game;

/** Default implementation of GameDao. */
final class DefaultGameDao extends AbstractDao<Game, GameDao> implements GameDao {
  @Inject
  DefaultGameDao(SessionFactory sessionFactory) {
    super(sessionFactory, Game.class);
  }
}
