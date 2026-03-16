import re

# Since BootstrapVerticleService is package-private, we can just extract the tests that test its specific functionality
# from VerticleLifecycleTest to BootstrapVerticleServiceTest.
# Actually, the startUp_validConfig_success, startUp_missingConfig_throwsRuntimeException, deploy_bootstrapNotStarted_throwsException
# in VerticleLifecycleTest are testing BootstrapVerticleService because they use VerticleServices.create().

content = '''package com.larpconnect.njall.init;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import io.vertx.core.AbstractVerticle;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BootstrapVerticleServiceTest {

  @Test
  public void deploy_notStarted_throwsException() {
    var lifecycle = VerticleServices.create(ImmutableList.of());
    assertThatThrownBy(() -> lifecycle.deploy(TestVerticle.class))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("BootstrapVerticleService not started");
  }

  @Test
  public void startUp_validConfig_success() throws Exception {
    var lifecycle =
        VerticleServices.create(
            ImmutableList.of(
                new com.larpconnect.njall.common.CommonModule(),
                new com.larpconnect.njall.common.codec.CodecModule()));

    lifecycle.startAsync().awaitRunning(10, TimeUnit.SECONDS);
    assertThat(lifecycle.isRunning()).isTrue();

    lifecycle.deploy(TestVerticle.class);

    lifecycle.stopAsync().awaitTerminated(10, TimeUnit.SECONDS);
    assertThat(lifecycle.isRunning()).isFalse();
  }

  @Test
  public void startUp_missingConfig_throwsRuntimeException() {
    System.setProperty("njall.config.resource", "missing.json");
    try {
      var lifecycle =
          VerticleServices.create(
              ImmutableList.of(
                  new com.larpconnect.njall.common.CommonModule(),
                  new com.larpconnect.njall.common.codec.CodecModule()));
      assertThatThrownBy(() -> lifecycle.startAsync().awaitRunning(10, TimeUnit.SECONDS))
          .isInstanceOf(IllegalStateException.class);
    } finally {
      System.clearProperty("njall.config.resource");
    }
  }

  static final class TestVerticle extends AbstractVerticle {}
}
'''
with open("init/src/test/java/com/larpconnect/njall/init/BootstrapVerticleServiceTest.java", "w") as f:
    f.write(content)

# Remove the tests from VerticleLifecycleTest
with open("init/src/test/java/com/larpconnect/njall/init/VerticleLifecycleTest.java", "r") as f:
    vlt_content = f.read()

vlt_content = re.sub(r'\s*@Test\s*public void deploy_bootstrapNotStarted_throwsException\(\) \{.*?"BootstrapVerticleService not started"\);\s*\}', '', vlt_content, flags=re.DOTALL)
vlt_content = re.sub(r'\s*@Test\s*public void startUp_validConfig_success\(\) throws Exception \{.*?\n\s*\}\n', '', vlt_content, flags=re.DOTALL)
vlt_content = re.sub(r'\s*@Test\s*public void startUp_missingConfig_throwsRuntimeException\(\) \{.*?\n\s*\}\n\s*\}\n', '', vlt_content, flags=re.DOTALL)

with open("init/src/test/java/com/larpconnect/njall/init/VerticleLifecycleTest.java", "w") as f:
    f.write(vlt_content)
