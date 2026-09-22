package com.larpconnect.njall.data.session;

import com.google.inject.Inject;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.hibernate.SessionFactory;

/** Tracks instantiated Hibernate session factories and provides bulk shutdown capability. */
public final class ActiveSessionFactories {

  private final Set<SessionFactory> factories = ConcurrentHashMap.newKeySet();

  @Inject
  ActiveSessionFactories() {}

  /**
   * Registers an instantiated {@link SessionFactory} for lifecycle management.
   *
   * @param sessionFactory The session factory to register.
   */
  public void register(SessionFactory sessionFactory) {
    if (sessionFactory != null) {
      factories.add(sessionFactory);
    }
  }

  /** Gracefully closes all registered and open session factories. */
  public void closeAll() {
    for (var factory : factories) {
      if (!factory.isClosed()) {
        factory.close();
      }
    }
    factories.clear();
  }

  int activeCount() {
    return factories.size();
  }
}
