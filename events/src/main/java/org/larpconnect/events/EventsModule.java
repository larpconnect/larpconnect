package org.larpconnect.events;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

/** Configures DI components for the events package. */
public final class EventsModule extends AbstractModule {
  @Override
  protected void configure() {
    Multibinder.newSetBinder(binder(), VerticleProvider.class);
  }
}
