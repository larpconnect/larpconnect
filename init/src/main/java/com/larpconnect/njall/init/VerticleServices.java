package com.larpconnect.njall.init;

import static com.larpconnect.njall.common.annotations.ContractTag.PURE;

import com.google.inject.Module;
import com.larpconnect.njall.common.annotations.AiContract;
import java.util.List;

public interface VerticleServices {
  @AiContract(
      require = "modules \\neq \\bot",
      ensure = {"$res \\neq \\bot", "$res \\text{ is a new VerticleLifecycle}"},
      tags = PURE,
      implementationHint = "Creates a new VerticleService instance with the provided modules.")
  static VerticleService create(List<Module> modules) {
    return new VerticleLifecycle(modules);
  }
}
