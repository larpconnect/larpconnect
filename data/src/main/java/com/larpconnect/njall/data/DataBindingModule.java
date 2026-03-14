package com.larpconnect.njall.data;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.persistence.Persistence;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.reactive.mutiny.Mutiny;

/** Internal bindings for the data module. */
final class DataBindingModule extends AbstractModule {

  DataBindingModule() {}

  @Override
  protected void configure() {}

  @Provides
  @Singleton
  Mutiny.SessionFactory provideSessionFactory(
      @Named("db.url") String url,
      @Named("db.user") String user,
      @Named("db.password") String password) {
    Map<String, String> properties = new HashMap<>();
    properties.put("jakarta.persistence.jdbc.url", url);
    properties.put("jakarta.persistence.jdbc.user", user);
    properties.put("jakarta.persistence.jdbc.password", password);
    properties.put("hibernate.connection.pool_size", "10");

    return Persistence.createEntityManagerFactory("larpconnect", properties)
        .unwrap(Mutiny.SessionFactory.class);
  }
}
