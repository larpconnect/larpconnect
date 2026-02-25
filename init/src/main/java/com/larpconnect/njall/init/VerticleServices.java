package com.larpconnect.njall.init;

import com.google.inject.Module;
import java.util.List;

public interface VerticleServices {
  static VerticleService create(List<Module> modules) {
    return new VerticleLifecycle(modules);
  }
}
