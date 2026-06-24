package org.larpconnect.data.schema;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/** Guice module for schema routing services. */
public final class SchemaModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(StudioRoutingService.class).to(DefaultStudioRoutingService.class).in(Singleton.class);
  }
}
