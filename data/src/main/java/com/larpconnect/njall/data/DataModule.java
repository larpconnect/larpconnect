package com.larpconnect.njall.data;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import jakarta.persistence.Persistence;
import org.hibernate.reactive.mutiny.Mutiny;

public final class DataModule extends AbstractModule {
  public DataModule() {}

  @Override
  protected void configure() {}

  @Provides
  @Singleton
  public Mutiny.SessionFactory provideSessionFactory() {
    return Persistence.createEntityManagerFactory("larpconnect")
        .unwrap(Mutiny.SessionFactory.class);
  }
}
