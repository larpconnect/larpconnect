package com.larpconnect.njall.api.studios.common;

import com.google.inject.BindingAnnotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Binding annotation for top-level studio {@code RouteProvider} multibindings. */
@BindingAnnotation
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
public @interface StudioSubRoute {}
