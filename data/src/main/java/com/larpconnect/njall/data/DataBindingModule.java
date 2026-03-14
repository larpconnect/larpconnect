package com.larpconnect.njall.data;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import jakarta.persistence.Persistence;
import org.hibernate.reactive.mutiny.Mutiny;

/** Bindings for the data module. */
final class DataBindingModule extends AbstractModule {

  /** Constructor. */
  public DataBindingModule() {}

  @Override
  protected void configure() {
    // Any specific bindings goes here.
  }

  @Provides
  @Singleton
  Mutiny.SessionFactory provideSessionFactory() {
    return Persistence.createEntityManagerFactory("njall-pu").unwrap(Mutiny.SessionFactory.class);
  }
}
