package com.larpconnect.njall.server.cli;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

final class CliRunnerTest {

  @Test
  @DisplayName("execute with no arguments defaults to ServerCommand and returns null")
  void execute_emptyArgs_defaultsToServerCommand() {
    var runner = new CliRunner();
    var exitCode = runner.execute(new String[0]);
    assertThat(exitCode).isNull();
  }

  @Test
  @DisplayName("execute with null args defaults to ServerCommand and returns null")
  void execute_nullArgs_defaultsToServerCommand() {
    var runner = new CliRunner();
    var exitCode = runner.execute(null);
    assertThat(exitCode).isNull();
  }

  @Test
  @DisplayName("execute with server subcommand returns null")
  void execute_serverSubcommand_returnsNull() {
    var runner = new CliRunner();
    var exitCode = runner.execute(new String[] {"server"});
    assertThat(exitCode).isNull();
  }

  @Test
  @DisplayName("execute with root help flag returns zero")
  void execute_rootHelp_returnsZero() {
    var runner = new CliRunner();
    var exitCode = runner.execute(new String[] {"--help"});
    assertThat(exitCode).isEqualTo(0);
  }

  @Test
  @DisplayName("execute with root version flag returns zero")
  void execute_rootVersion_returnsZero() {
    var runner = new CliRunner();
    var exitCode = runner.execute(new String[] {"-V"});
    assertThat(exitCode).isEqualTo(0);
  }

  @Test
  @DisplayName("execute with subcommand help flag returns zero")
  void execute_subcommandHelp_returnsZero() {
    var runner = new CliRunner();
    var exitCode = runner.execute(new String[] {"server", "--help"});
    assertThat(exitCode).isEqualTo(0);
  }

  @Test
  @DisplayName("execute with subcommand version flag returns zero")
  void execute_subcommandVersion_returnsZero() {
    var runner = new CliRunner();
    var exitCode = runner.execute(new String[] {"server", "-V"});
    assertThat(exitCode).isEqualTo(0);
  }

  @Test
  @DisplayName("execute with delegate constructor routes correctly to server and migrate")
  void execute_withDelegates_routesCorrectly() {
    var serverLaunched = new AtomicBoolean(false);
    var migrateCount = new AtomicInteger(0);

    var runner =
        new CliRunner(cfg -> serverLaunched.set(true), cfg -> migrateCount.incrementAndGet());

    var serverExit = runner.execute(new String[] {"server"});
    assertThat(serverExit).isNull();
    assertThat(serverLaunched.get()).isTrue();

    var migrateExit = runner.execute(new String[] {"migrate"});
    assertThat(migrateExit).isEqualTo(1);
  }

  @Test
  @DisplayName("execute with command instances constructor routes correctly")
  void execute_withCommandInstances_routesCorrectly() {
    var serverCmd = new ServerCommand(cfg -> {});
    var migrateCmd = new MigrateCommand(cfg -> 42);

    var runner = new CliRunner(serverCmd, migrateCmd);

    var exitCode = runner.execute(new String[] {"migrate"});
    assertThat(exitCode).isEqualTo(42);
  }

  @Test
  @DisplayName("execute with unknown option returns exit code 2")
  void execute_unknownOption_returnsTwo() {
    var runner = new CliRunner();
    var exitCode = runner.execute(new String[] {"--nonexistent-flag"});
    assertThat(exitCode).isEqualTo(CommandLine.ExitCode.USAGE);
  }

  @Test
  @DisplayName("CliModule provides valid CliRunner instance")
  void cliModule_providesCliRunner() {
    var injector = Guice.createInjector(new CliModule());
    var runner = injector.getInstance(CliRunner.class);
    assertThat(runner).isNotNull();
  }
}
