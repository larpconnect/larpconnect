package org.larpconnect.api;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.multibindings.ProvidesIntoSet;
import org.larpconnect.events.VerticleProvider;

/** Configures DI components for the api package. */
public final class ApiModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(ApiVerticle.class);
  }

  @ProvidesIntoSet
  VerticleProvider provideApiVerticle(Provider<ApiVerticle> verticleProvider) {
    return verticleProvider::get;
  }
}
