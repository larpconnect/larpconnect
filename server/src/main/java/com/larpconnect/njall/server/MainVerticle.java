package com.larpconnect.njall.server;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import io.vertx.core.Verticle;

/**
 * A marker interface indicating the root verticle from which all other application verticles are
 * launched.
 *
 * <p>By depending on an interface instead of a concrete class, the initialization code (like {@code
 * com.larpconnect.njall.init.VerticleLifecycle}) can blindly deploy the bound {@code MainVerticle}
 * class without coupling the generic initialization logic to the specific modules or verticles
 * needed by the application server.
 */
@DefaultImplementation(DefaultMainVerticle.class)
public interface MainVerticle extends Verticle {}
