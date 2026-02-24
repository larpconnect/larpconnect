package com.larpconnect.init;

import io.vertx.core.Vertx;

/** Registry for Protobuf codecs. */
public interface ProtoCodecRegistry {
  /**
   * Registers Protobuf codecs with the given Vertx instance.
   *
   * @param vertx the Vertx instance
   */
  void register(Vertx vertx);
}
