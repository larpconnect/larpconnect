package com.larpconnect.common.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Defines the "Logical Physics" of a method. This annotation is a Hard Invariant. */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface AiContract {
  /** Preconditions. You must verify these are true before the call. */
  String[] require() default {};

  /** Postconditions. You must guarantee these are true upon exit. */
  String[] ensure() default {};

  /** Conditions that must remain true throughout execution. */
  String[] invariants() default {};

  /** Advisory statements on the nature of the method. */
  ContractTag[] tags() default {};

  /** Intent communication. A high-level semantic anchor for the "Reasoning Path" of the method. */
  String implementationHint() default "";
}
