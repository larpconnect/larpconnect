with open("init/src/main/java/com/larpconnect/njall/init/VerticleLifecycle.java", "r") as f:
    content = f.read()

import re

# Update imports
if "import jakarta.inject.Provider;" not in content:
    content = content.replace("import jakarta.inject.Singleton;", "import jakarta.inject.Provider;\nimport jakarta.inject.Singleton;")

# Update fields
content = content.replace("private final Vertx vertx;", "private final Provider<Vertx> vertxProvider;")
content = content.replace("private final VerticleSetupService setupService;", "private final Provider<VerticleSetupService> setupServiceProvider;")

# Update constructor
content = re.sub(
    r'@Inject\s*VerticleLifecycle\(Vertx vertx,\s*VerticleSetupService setupService,\s*Injector injector\) \{\s*this\.vertx = vertx;\s*this\.setupService = setupService;\s*this\.setupService\.setup\(vertx, injector\);\s*\}',
    '''@Inject
  VerticleLifecycle(Provider<Vertx> vertxProvider, Provider<VerticleSetupService> setupServiceProvider, Injector injector) {
    this.vertxProvider = vertxProvider;
    this.setupServiceProvider = setupServiceProvider;
    this.setupServiceProvider.get().setup(vertxProvider.get(), injector);
  }''',
    content
)

# Update shutDown
content = content.replace("vertx\n        .close()", "vertxProvider.get()\n        .close()")
content = content.replace("vertx.close()", "vertxProvider.get().close()")

# Update deploy
content = content.replace("setupService.deploy(verticleClass);", "setupServiceProvider.get().deploy(verticleClass);")

with open("init/src/main/java/com/larpconnect/njall/init/VerticleLifecycle.java", "w") as f:
    f.write(content)
