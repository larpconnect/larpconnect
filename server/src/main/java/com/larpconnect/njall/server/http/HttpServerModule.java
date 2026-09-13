package com.larpconnect.njall.server.http;

import com.google.inject.AbstractModule;

/** Guice module binding HTTP server infrastructure components. */
public final class HttpServerModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(HttpServerService.class).to(DefaultHttpServerService.class);
  }
}
