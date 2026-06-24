package org.larpconnect.base;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.multibindings.ProvidesIntoSet;
import org.larpconnect.events.VerticleProvider;

/** Configures DI components for the base package. */
public final class BaseModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(BaseVerticle.class);
  }

  @ProvidesIntoSet
  VerticleProvider provideBaseVerticle(Provider<BaseVerticle> verticleProvider) {
    return verticleProvider::get;
  }
}
