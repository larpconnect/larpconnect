package com.larpconnect.njall.common.codec;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.proto.MessageRequest;
import io.vertx.core.eventbus.MessageCodec;

/**
 * A strongly-typed interface for the {@link MessageCodec} that handles {@link MessageRequest}
 * objects over the Vert.x event bus.
 *
 * <p>By providing a specific interface, we enable Guice to inject this codec deterministically
 * without relying on type literals or string-based lookup. This ensures that any component
 * requiring the ability to serialize, deserialize, or transform protocol buffer messages has a
 * clear, type-safe dependency.
 */
@DefaultImplementation(ProtoCodecRegistry.class)
public interface ProtoCodec extends MessageCodec<MessageRequest, MessageRequest> {}
