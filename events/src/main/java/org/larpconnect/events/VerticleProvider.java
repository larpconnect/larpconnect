package org.larpconnect.events;

import io.vertx.core.Verticle;
import java.util.function.Supplier;

/** Provider interface for dynamically registering Vert.x Verticles. */
public interface VerticleProvider extends Supplier<Verticle> {}
