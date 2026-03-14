package com.larpconnect.njall.server.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Guice qualifier annotation used to identify the path or filename of the OpenAPI specification.
 *
 * <p>By using a dedicated qualifier rather than binding to a generic string or using named
 * bindings, the injection of the OpenAPI specification configuration value becomes type-safe and
 * robust against spelling errors. It ensures that the Vert.x web server initializes its routes
 * using the correct contract definition.
 */
@Qualifier
@Retention(RUNTIME)
@Target({FIELD, PARAMETER, METHOD})
public @interface OpenApiSpec {}
