package com.larpconnect.njall.common.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the "Logical Physics" of a method or component using mathematical or logical notation.
 *
 * <p>This annotation acts as an ersatz form of Design by Contract. It explicitly outlines the
 * preconditions, postconditions, and invariants of a method or class. Its primary architectural
 * purpose is to guarantee what must be true before and after execution, thereby providing a robust
 * context for AI agents that might otherwise struggle to trace execution flows or trust system
 * invariants in a highly asynchronous, event-driven architecture.
 */
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
