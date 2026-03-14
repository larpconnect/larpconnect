package com.larpconnect.njall.data;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
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
  Mutiny.SessionFactory provideSessionFactory() {
    // Here we could inject config instead of hardcoding
    Map<String, String> properties = new HashMap<>();
    properties.put("jakarta.persistence.jdbc.url", "jdbc:postgresql://localhost:5432/test");
    properties.put("jakarta.persistence.jdbc.user", "test");
    properties.put("jakarta.persistence.jdbc.password", "test");

    return Persistence.createEntityManagerFactory("larpconnect", properties)
        .unwrap(Mutiny.SessionFactory.class);
  }
}
