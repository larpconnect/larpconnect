package com.larpconnect.njall.common.annotations;

import com.google.inject.Module;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Directs automated agents to the Guice module required to instantiate a class that has a private
 * or package-private constructor.
 *
 * <p>Its architectural purpose is to signal to agents that they cannot directly instantiate the
 * target object and must instead rely on the specified Guice module to obtain it, preserving
 * encapsulation while enabling dependency injection.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BuildWith {
  /** The public Guice module for this class. */
  Class<? extends Module> value();
}
