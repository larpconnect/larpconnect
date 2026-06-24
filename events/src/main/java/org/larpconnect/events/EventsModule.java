package org.larpconnect.events;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import io.vertx.core.Vertx;

/** Configures DI components for the events package. */
public final class EventsModule extends AbstractModule {
  @Override
  protected void configure() {
    Multibinder.newSetBinder(binder(), VerticleProvider.class);

    // Bind VertxProvider as a Singleton
    bind(VertxProvider.class).in(Singleton.class);
    // Bind Vertx class to be resolved via VertxProvider
    bind(Vertx.class).toProvider(VertxProvider.class).in(Singleton.class);
    // Bind GuiceVerticleFactory
    bind(GuiceVerticleFactory.class).in(Singleton.class);
  }
}
