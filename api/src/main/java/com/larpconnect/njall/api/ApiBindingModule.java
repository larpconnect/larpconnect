package com.larpconnect.njall.api;

import com.google.inject.AbstractModule;
import com.google.protobuf.util.JsonFormat;
import com.larpconnect.njall.common.annotations.InstallInstead;

@InstallInstead(ApiModule.class)
final class ApiBindingModule extends AbstractModule {
  ApiBindingModule() {}

  @Override
  protected void configure() {
    bind(ApiObjectParser.class).to(DefaultApiObjectParser.class);
    bind(JsonFormat.Printer.class)
        .toInstance(
            JsonFormat.printer().preservingProtoFieldNames().omittingInsignificantWhitespace());
    bind(JsonFormat.Parser.class).toInstance(JsonFormat.parser().ignoringUnknownFields());
  }
}
