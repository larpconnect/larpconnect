package com.larpconnect.njall.server.cli;

import static org.assertj.core.api.Assertions.assertThat;

import com.typesafe.config.Config;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

final class ServerCommandTest {

  @Test
  @DisplayName("server options parse correctly")
  void parse_serverOptions() {
    var serverCommand = new ServerCommand();
    var cmd = new CommandLine(serverCommand);

    cmd.parseArgs(
        "--host", "127.0.0.1",
        "-p", "9090",
        "--name", "alpha-node",
        "--primary-domain", "larpconnect.test",
        "--admin-contact", "ops@larpconnect.test");

    assertThat(serverCommand.host()).contains("127.0.0.1");
    assertThat(serverCommand.port()).contains(9090);
    assertThat(serverCommand.name()).contains("alpha-node");
    assertThat(serverCommand.primaryDomain()).contains("larpconnect.test");
    assertThat(serverCommand.adminContact()).contains("ops@larpconnect.test");
  }

  @Test
  @DisplayName("call executes server launcher with layered config and returns null")
  void call_executesLauncherAndReturnsNull() {
    var capturedConfig = new AtomicReference<Config>();
    var serverCommand = new ServerCommand(capturedConfig::set);
    var cmd = new CommandLine(serverCommand);
    cmd.parseArgs("--host", "127.0.0.1", "-p", "8888");

    var exitCode = serverCommand.call();

    assertThat(exitCode).isNull();
    assertThat(capturedConfig.get()).isNotNull();
    assertThat(capturedConfig.get().getString("larpconnect.server.host")).isEqualTo("127.0.0.1");
    assertThat(capturedConfig.get().getInt("larpconnect.server.port")).isEqualTo(8888);
  }

  @Test
  @DisplayName("callWithRoot resolves root config file and builds layered config")
  void callWithRoot_resolvesRootConfig() throws Exception {
    var capturedConfig = new AtomicReference<Config>();
    var serverCommand = new ServerCommand(capturedConfig::set);

    var tempFile = Files.createTempFile("server-test", ".conf");
    try {
      Files.writeString(tempFile, "larpconnect.server.name = \"file-node\"\n");

      var runner = new CliRunner(serverCommand, new MigrateCommand());
      var exitCode =
          runner.execute(new String[] {"-c", tempFile.toString(), "server", "--host", "10.0.0.1"});

      assertThat(exitCode).isNull();
      assertThat(capturedConfig.get()).isNotNull();
      assertThat(capturedConfig.get().getString("larpconnect.server.name")).isEqualTo("file-node");
      assertThat(capturedConfig.get().getString("larpconnect.server.host")).isEqualTo("10.0.0.1");
    } finally {
      Files.deleteIfExists(tempFile);
    }
  }

  @Test
  @DisplayName("default constructor initializes valid ServerCommand")
  void defaultConstructor_initializes() {
    var serverCommand = new ServerCommand();
    assertThat(serverCommand).isNotNull();
    assertThat(serverCommand.host()).isEmpty();
    assertThat(serverCommand.port()).isEmpty();
    assertThat(serverCommand.name()).isEmpty();
    assertThat(serverCommand.primaryDomain()).isEmpty();
    assertThat(serverCommand.adminContact()).isEmpty();
    assertThat(serverCommand.call()).isNull();
  }
}
