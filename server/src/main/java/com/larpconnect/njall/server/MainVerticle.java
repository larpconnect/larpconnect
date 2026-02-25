package com.larpconnect.njall.server;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import io.vertx.core.Verticle;

@DefaultImplementation(DefaultMainVerticle.class)
public interface MainVerticle extends Verticle {}
