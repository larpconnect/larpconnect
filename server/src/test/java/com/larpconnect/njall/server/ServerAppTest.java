package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.Service;
import com.google.inject.Injector;
import com.larpconnect.njall.data.migration.DatabaseMigrator;
import com.larpconnect.njall.server.cli.CliRunner;
import com.larpconnect.njall.server.http.HttpServerService;
import com.typesafe.config.Config;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.http.javadsl.ServerBinding;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class ServerAppTest {

  @Test
  @DisplayName("runWithArgs delegates to CliRunner and returns its exit code")
  void runWithArgs_delegatesToCliRunner() {
    var cliRunner = mock(CliRunner.class);
    when(cliRunner.execute(new String[] {"migrate"})).thenReturn(0);

    var app = new ServerApp(cliRunner);
    var exitCode = app.runWithArgs(new String[] {"migrate"});

    assertThat(exitCode).isEqualTo(0);
    verify(cliRunner).execute(new String[] {"migrate"});
  }

  @Test
  @DisplayName("runWithArgs returns null when CliRunner returns null")
  void runWithArgs_whenCliRunnerReturnsNull_returnsNull() {
    var cliRunner = mock(CliRunner.class);
    when(cliRunner.execute(new String[] {"server"})).thenReturn(null);

    var app = new ServerApp(cliRunner);
    var exitCode = app.runWithArgs(new String[] {"server"});

    assertThat(exitCode).isNull();
  }

  @Test
  @DisplayName("default constructor initializes valid ServerApp")
  void constructor_default_initializesApp() {
    var app = new ServerApp();
    assertThat(app).isNotNull();
    assertThat(app.runWithArgs(new String[] {"--help"})).isEqualTo(0);
  }

  @Test
  @DisplayName("createDefaultCliRunner produces executable CliRunner")
  void createDefaultCliRunner_producesValidRunner() {
    var app = new ServerApp();
    var runner = app.createDefaultCliRunner();
    assertThat(runner).isNotNull();
    assertThat(runner.execute(new String[] {"--help"})).isEqualTo(0);
  }

  @Test
  @DisplayName("launchServer starts ServerManagerService and awaits running state")
  void launchServer_startsServerManagerServiceAsync() throws Exception {
    var mockConfig = mock(Config.class);
    var mockInjector = mock(Injector.class);
    var mockRegistrar = mock(ShutdownHookRegistrar.class);
    ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "server-app-test");
    var mockHttpService = mock(HttpServerService.class);
    var binding = mock(ServerBinding.class);

    when(mockHttpService.start()).thenReturn(CompletableFuture.completedFuture(binding));

    var realService = new DefaultServerManagerService(mockRegistrar, system, mockHttpService);
    when(mockInjector.getInstance(ServerManagerService.class)).thenReturn(realService);

    var app = spy(new ServerApp());
    doReturn(mockInjector).when(app).createInjector(mockConfig);

    try {
      app.launchServer(mockConfig);

      assertThat(realService.state()).isEqualTo(Service.State.RUNNING);
    } finally {
      if (realService.isRunning()) {
        realService.stopAsync().awaitTerminated(5, TimeUnit.SECONDS);
      }
      system.terminate();
      system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }
  }

  @Test
  @DisplayName("launchServer throws IllegalStateException when ServerManagerService fails to start")
  void launchServer_whenStartupFails_throwsException() throws Exception {
    var mockConfig = mock(Config.class);
    var mockInjector = mock(Injector.class);
    var mockRegistrar = mock(ShutdownHookRegistrar.class);
    ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "server-app-fail-test");
    var mockHttpService = mock(HttpServerService.class);

    when(mockHttpService.start())
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("bind failure")));

    var realService = new DefaultServerManagerService(mockRegistrar, system, mockHttpService);
    when(mockInjector.getInstance(ServerManagerService.class)).thenReturn(realService);

    var app = spy(new ServerApp());
    doReturn(mockInjector).when(app).createInjector(mockConfig);

    try {
      assertThatIllegalStateException().isThrownBy(() -> app.launchServer(mockConfig));
      assertThat(realService.state()).isEqualTo(Service.State.FAILED);
    } finally {
      system.terminate();
      system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }
  }

  @Test
  @DisplayName("executeMigration returns zero on successful migration")
  void executeMigration_whenSuccessful_returnsZero() throws Exception {
    var mockConfig = mock(Config.class);
    var mockInjector = mock(Injector.class);
    var mockMigrator = mock(DatabaseMigrator.class);

    when(mockInjector.getInstance(DatabaseMigrator.class)).thenReturn(mockMigrator);

    var app = spy(new ServerApp());
    doReturn(mockInjector).when(app).createInjector(mockConfig);

    var exitCode = app.executeMigration(mockConfig);

    assertThat(exitCode).isEqualTo(0);
    verify(mockMigrator).migrate();
  }

  @Test
  @DisplayName("executeMigration returns one on migration failure")
  void executeMigration_whenFails_returnsOne() throws Exception {
    var mockConfig = mock(Config.class);
    var mockInjector = mock(Injector.class);
    var mockMigrator = mock(DatabaseMigrator.class);

    doThrow(new RuntimeException("migration error")).when(mockMigrator).migrate();
    when(mockInjector.getInstance(DatabaseMigrator.class)).thenReturn(mockMigrator);

    var app = spy(new ServerApp());
    doReturn(mockInjector).when(app).createInjector(mockConfig);

    var exitCode = app.executeMigration(mockConfig);

    assertThat(exitCode).isEqualTo(1);
  }
}
