package com.larpconnect.njall.common.annotations;

import com.google.inject.Module;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Signals a delegated module installation. */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface InstallInstead {
  /** The target module class to install instead. */
  Class<? extends Module> value();
}
