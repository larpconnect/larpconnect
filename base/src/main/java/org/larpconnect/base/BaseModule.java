package org.larpconnect.base;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.larpconnect.events.VerticleProvider;

/** Configures DI components for the base package. */
public final class BaseModule extends AbstractModule {
  @Override
  protected void configure() {
    Multibinder.newSetBinder(binder(), VerticleProvider.class)
        .addBinding()
        .to(BaseVerticleProvider.class);
  }
}
