package com.larpconnect.njall.server.cli;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

final class RootCommandTest {

  @Test
  @DisplayName("root command defines expected subcommands")
  void rootCommand_definesSubcommands() {
    var root = new RootCommand();
    var cmd = new CommandLine(root);

    var subcommands = cmd.getSubcommands();

    assertThat(subcommands).containsKeys("server", "migrate");
    Object serverCmd = subcommands.get("server").getCommand();
    Object migrateCmd = subcommands.get("migrate").getCommand();
    assertThat(serverCmd).isInstanceOf(ServerCommand.class);
    assertThat(migrateCmd).isInstanceOf(MigrateCommand.class);
  }

  @Test
  @DisplayName("root command call delegates to default server command and returns null")
  void call_returnsNull() {
    var root = new RootCommand();
    assertThat(root.call()).isNull();
    assertThat(root.configFile()).isNull();
    assertThat(root.verbose()).isFalse();
  }

  @Test
  @DisplayName("global options parse correctly")
  void parse_globalOptions() {
    var root = new RootCommand();
    var cmd = new CommandLine(root);

    cmd.parseArgs("-v", "--config", "test.conf");

    assertThat(root.verbose()).isTrue();
    assertThat(root.configFile()).hasName("test.conf");
  }
}
