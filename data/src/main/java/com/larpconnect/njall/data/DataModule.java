package com.larpconnect.njall.data;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.larpconnect.njall.data.dao.DaoModule;
import jakarta.persistence.Persistence;
import org.hibernate.reactive.mutiny.Mutiny;

public final class DataModule extends AbstractModule {
  public DataModule() {}

  @Override
  protected void configure() {
    install(new DaoModule());
  }

  @Provides
  @Singleton
  public Mutiny.SessionFactory provideSessionFactory() {
    // Ideally this uses configuration from vertx:
    // String url = config.getString("database.url");
    // String user = config.getString("database.user");
    // String pass = config.getString("database.pass");
    return Persistence.createEntityManagerFactory("larpconnect")
        .unwrap(Mutiny.SessionFactory.class);
  }
}
