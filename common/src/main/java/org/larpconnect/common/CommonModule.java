package org.larpconnect.common;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/** Exposes the bindings for the common library. */
public final class CommonModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(Environment.class).to(SystemEnvironment.class).in(Singleton.class);
  }
}
