package org.larpconnect.data.hibernate;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.hibernate.SessionFactory;

/** Guice module for Hibernate lifecycle and SessionFactory bindings. */
public final class HibernateModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(HibernateService.class).to(DefaultHibernateService.class).in(Singleton.class);
  }

  @Provides
  @Singleton
  SessionFactory provideSessionFactory(HibernateService hibernateService) {
    return hibernateService.getSessionFactory();
  }
}
