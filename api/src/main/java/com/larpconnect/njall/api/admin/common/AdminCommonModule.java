package com.larpconnect.njall.api.admin.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

/** Guice module providing common administrative dependencies such as JSON object mappers. */
public final class AdminCommonModule extends AbstractModule {

  @Override
  protected void configure() {
    // No specific interface-to-implementation bindings required in common
  }

  @Provides
  @Singleton
  ObjectMapper provideObjectMapper() {
    return JsonMapper.builder()
        .findAndAddModules()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build();
  }
}
