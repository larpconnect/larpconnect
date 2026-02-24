package com.larpconnect.init;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Registry for Protobuf codecs. */
public class ProtoCodecRegistry {
  private static final Logger logger = LoggerFactory.getLogger(ProtoCodecRegistry.class);

  public ProtoCodecRegistry() {}

  /**
   * Registers Protobuf codecs with the given Vertx instance.
   *
   * @param vertx the Vertx instance
   */
  public void register(Vertx vertx) {
    logger.info("Registering Protobuf codecs");
    // TODO: Register actual codecs when protos are available.
  }
}
