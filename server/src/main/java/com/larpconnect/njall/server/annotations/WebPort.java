package com.larpconnect.njall.server.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Guice qualifier annotation used to identify the listening port for the main web server verticle.
 *
 * <p>Using a strongly-typed qualifier avoids the ambiguity of injecting generic integers. This
 * enforces that the server port configuration value, whether derived from environment variables or
 * JSON configuration, is deterministically provided to the component responsible for binding the
 * HTTP server, decoupling configuration parsing from the Verticle implementation.
 */
@Qualifier
@Retention(RUNTIME)
@Target({FIELD, PARAMETER, METHOD})
public @interface WebPort {}
