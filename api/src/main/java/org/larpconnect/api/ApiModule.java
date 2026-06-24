package org.larpconnect.api;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.larpconnect.events.VerticleProvider;

/** Configures DI components for the api package. */
public final class ApiModule extends AbstractModule {
  @Override
  protected void configure() {
    Multibinder.newSetBinder(binder(), VerticleProvider.class)
        .addBinding()
        .to(ApiVerticleProvider.class);
  }
}
