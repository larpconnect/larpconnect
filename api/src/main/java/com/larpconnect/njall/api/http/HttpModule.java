package com.larpconnect.njall.api.http;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;

/** Guice module binding HTTP routes and handlers for the API layer. */
public final class HttpModule extends AbstractModule {

  @Override
  protected void configure() {
    Multibinder.newSetBinder(binder(), RouteProvider.class);
    bind(TracingDirective.class).to(DefaultTracingDirective.class).in(Scopes.SINGLETON);
    bind(RootRoute.class).to(DefaultRootRoute.class);
  }
}
