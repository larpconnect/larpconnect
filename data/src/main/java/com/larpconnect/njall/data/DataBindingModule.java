package com.larpconnect.njall.data;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.larpconnect.njall.common.annotations.InstallInstead;
import jakarta.inject.Singleton;
import jakarta.persistence.Persistence;
import org.hibernate.reactive.mutiny.Mutiny;

@InstallInstead(DataModule.class)
final class DataBindingModule extends AbstractModule {
  DataBindingModule() {}

  @Override
  protected void configure() {}

  @Provides
  @Singleton
  Mutiny.SessionFactory provideSessionFactory() {
    // Create session factory here based on persistence unit named "njall-pu"
    return Persistence.createEntityManagerFactory("njall-pu").unwrap(Mutiny.SessionFactory.class);
  }
}
