package com.larpconnect.njall.api;

import com.google.inject.AbstractModule;
import com.google.protobuf.util.JsonFormat;

/**
 * Guice module for the API module. Exposing it allows other modules to install it and bypasses the
 * ArchUnit public class restriction which permits Modules to be public.
 */
public final class ApiModule extends AbstractModule {
  public ApiModule() {}

  @Override
  protected void configure() {
    bind(ApiObjectParser.class).to(DefaultApiObjectParser.class);
    bind(JsonFormat.Printer.class)
        .toInstance(
            JsonFormat.printer().preservingProtoFieldNames().omittingInsignificantWhitespace());
    bind(JsonFormat.Parser.class).toInstance(JsonFormat.parser().ignoringUnknownFields());
  }
}
