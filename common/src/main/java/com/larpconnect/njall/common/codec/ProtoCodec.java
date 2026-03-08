package com.larpconnect.njall.common.codec;

import com.larpconnect.njall.proto.MessageRequest;
import io.vertx.core.eventbus.MessageCodec;

public interface ProtoCodec extends MessageCodec<MessageRequest, MessageRequest> {}
