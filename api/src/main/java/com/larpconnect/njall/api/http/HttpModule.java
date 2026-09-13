package com.larpconnect.njall.api.http;

import com.google.inject.AbstractModule;

/** Guice module binding HTTP routes and handlers for the API layer. */
public final class HttpModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(RootRoute.class).to(DefaultRootRoute.class);
  }
}
