package com.larpconnect.njall.common.codec;

import com.google.inject.AbstractModule;
import com.larpconnect.njall.common.annotations.InstallInstead;

@InstallInstead(CodecModule.class)
final class CodecBindingModule extends AbstractModule {
  CodecBindingModule() {}

  @Override
  protected void configure() {
    bind(ProtoCodec.class).to(ProtoCodecRegistry.class);
  }
}
