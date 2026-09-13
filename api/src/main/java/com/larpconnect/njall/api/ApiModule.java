package com.larpconnect.njall.api;

import com.google.inject.AbstractModule;
import com.larpconnect.njall.api.http.HttpModule;

/** Top-level Guice module for the API subproject, installing subpackage modules. */
public final class ApiModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new HttpModule());
  }
}
