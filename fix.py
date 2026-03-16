with open("init/src/main/java/com/larpconnect/njall/init/VerticleLifecycle.java", "r") as f:
    content = f.read()

import re

# Instead of using Provider.get() for setup, I should just use the exact same instance.
# But the reviewer said: "Always, always inject vertx as a provider with Provider<Vertx> and then resolve it at the moment it is needed."
# He only mentioned `vertx`! He didn't say I should inject `setupService` as a provider!
# So I should inject `VerticleSetupService` DIRECTLY as a singleton, or just inject `VerticleSetupService` and keep the instance!
# Let me change `setupServiceProvider` to `setupService` directly.

content = content.replace("private final Provider<VerticleSetupService> setupServiceProvider;", "private final VerticleSetupService setupService;")
content = content.replace("Provider<VerticleSetupService> setupServiceProvider,", "VerticleSetupService setupService,")
content = content.replace("this.setupServiceProvider = setupServiceProvider;", "this.setupService = setupService;")
content = content.replace("this.setupServiceProvider.get().setup(vertxProvider.get(), injector);", "this.setupService.setup(vertxProvider.get(), injector);")
content = content.replace("setupServiceProvider.get().deploy(verticleClass);", "setupService.deploy(verticleClass);")

with open("init/src/main/java/com/larpconnect/njall/init/VerticleLifecycle.java", "w") as f:
    f.write(content)

with open("init/src/test/java/com/larpconnect/njall/init/VerticleLifecycleTest.java", "r") as f:
    test_content = f.read()

test_content = test_content.replace("new VerticleLifecycle(() -> mockVertx, () -> mockSetupService, mockInjector)", "new VerticleLifecycle(() -> mockVertx, mockSetupService, mockInjector)")

with open("init/src/test/java/com/larpconnect/njall/init/VerticleLifecycleTest.java", "w") as f:
    f.write(test_content)
