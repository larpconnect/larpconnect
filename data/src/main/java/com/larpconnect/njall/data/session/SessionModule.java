package com.larpconnect.njall.data.session;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.larpconnect.njall.data.annotation.NjallAdmin;
import com.larpconnect.njall.data.annotation.NjallUsers;
import com.larpconnect.njall.data.config.SessionConfig;
import java.util.Set;
import org.hibernate.SessionFactory;

/** Guice module configuring Hibernate SessionFactory creation and role-scoped session bindings. */
public final class SessionModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(SessionFactoryFactory.class).to(DefaultSessionFactoryFactory.class);
    bind(ActiveSessionFactories.class).in(Scopes.SINGLETON);
    // Initialize multibinders for mapped JPA entity classes
    Multibinder.newSetBinder(binder(), new TypeLiteral<Class<?>>() {}, NjallAdmin.class);
    Multibinder.newSetBinder(binder(), new TypeLiteral<Class<?>>() {}, NjallUsers.class);
  }

  @Provides
  @Singleton
  @NjallAdmin
  SessionFactory provideAdminSessionFactory(
      SessionFactoryFactory factory,
      @NjallAdmin SessionConfig config,
      @NjallAdmin Set<Class<?>> annotatedClasses,
      ActiveSessionFactories activeFactories) {
    var sessionFactory = factory.create(config, annotatedClasses);
    activeFactories.register(sessionFactory);
    return sessionFactory;
  }

  @Provides
  @Singleton
  @NjallUsers
  SessionFactory provideUsersSessionFactory(
      SessionFactoryFactory factory,
      @NjallUsers SessionConfig config,
      @NjallUsers Set<Class<?>> annotatedClasses,
      ActiveSessionFactories activeFactories) {
    var sessionFactory = factory.create(config, annotatedClasses);
    activeFactories.register(sessionFactory);
    return sessionFactory;
  }
}
