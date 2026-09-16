package com.larpconnect.njall.data.session;

import com.larpconnect.njall.data.config.SessionConfig;
import java.util.Collection;
import org.hibernate.SessionFactory;

/** Factory for creating configured Hibernate {@link SessionFactory} instances. */
public interface SessionFactoryFactory {

  /**
   * Builds and configures a {@link SessionFactory} for the given session configuration and mapped
   * entity classes.
   *
   * @param config The session configuration profile.
   * @param annotatedClasses Collection of JPA annotated entity classes.
   * @return A configured, active {@link SessionFactory}.
   */
  SessionFactory create(SessionConfig config, Collection<Class<?>> annotatedClasses);
}
