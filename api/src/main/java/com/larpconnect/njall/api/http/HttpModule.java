package com.larpconnect.njall.api.http;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.larpconnect.njall.api.RouteProvider;

/** Guice module binding HTTP routes and handlers for the API layer. */
public final class HttpModule extends AbstractModule {

  @Override
  protected void configure() {
    Multibinder.newSetBinder(binder(), RouteProvider.class);
    bind(RootRoute.class).to(DefaultRootRoute.class);
  }
}
