package com.larpconnect.njall.common.annotations;

import com.google.inject.Module;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Signals that an automated agent should look for a binding in the specified module instead of the
 * annotated one.
 *
 * <p>Its architectural purpose is to bridge the gap caused by the "one public module per package"
 * rule. Agents often find a binding in a package-private module but become confused about how to
 * install it. This annotation directs them to the public module that installs the package-private
 * module, preserving the directed acyclic graph of Guice dependencies.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface InstallInstead {
  /** The target module class to install instead. */
  Class<? extends Module> value();
}
