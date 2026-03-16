package com.larpconnect.njall.init;

import static com.larpconnect.njall.common.annotations.ContractTag.PURE;

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;
import com.larpconnect.njall.common.annotations.AiContract;

/**
 * A static factory for instantiating the {@link VerticleService} implementation.
 *
 * <p>Abstracting the creation into this factory interface prevents calling code (like the main
 * entry point) from needing direct access to the concrete {@code VerticleLifecycle} class,
 * preserving encapsulation within the {@code init} module.
 */
public interface VerticleServices {

  /**
   * Instantiates a new {@code VerticleService} that manages the provided Guice modules.
   *
   * @param modules an immutable list of Guice modules used to configure the application's bindings
   * @return a new, unstarted VerticleService instance
   */
  @AiContract(
      require = "modules \\neq \\bot",
      ensure = {"$res \\neq \\bot", "$res \\text{ is a new VerticleLifecycle}"},
      tags = PURE,
      implementationHint = "Creates a new VerticleService instance with the provided modules.")
  static VerticleService create(ImmutableList<Module> modules) {
    return new BootstrapVerticleService(modules);
  }
}
