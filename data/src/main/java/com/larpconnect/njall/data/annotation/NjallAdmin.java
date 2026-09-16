package com.larpconnect.njall.data.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.inject.BindingAnnotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Guice binding annotation qualifying persistence resources scoped to the {@code njall_admin} role.
 */
@BindingAnnotation
@Retention(RUNTIME)
@Target({FIELD, PARAMETER, METHOD})
public @interface NjallAdmin {}
