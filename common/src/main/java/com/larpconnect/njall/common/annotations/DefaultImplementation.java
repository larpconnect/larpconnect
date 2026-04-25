package com.larpconnect.njall.common.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates the expected concrete class for an interface when there is primarily a single
 * implementation.
 *
 * <p>Its architectural purpose is to guide automated agents to the concrete implementation without
 * polluting the interface with direct dependencies, ensuring that the explicit bindings defined in
 * Guice modules remain the single source of truth while still offering discoverability during
 * context engineering.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DefaultImplementation {
  /** The default implementation class. */
  Class<?> value();
}
