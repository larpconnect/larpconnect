package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.larpconnect.njall.server.cli.CliRunner;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.pekko.actor.typed.ActorSystem;
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
  @DisplayName("registerShutdownHook registers hook thread when ActorSystem present")
  void registerShutdownHook_registersHook() {
    var injector = mock(Injector.class);
    @SuppressWarnings("unchecked")
    // Safe mock of typed ActorSystem for shutdown hook test
    ActorSystem<Void> actorSystem = mock(ActorSystem.class);
    when(injector.getInstance(Key.get(new TypeLiteral<ActorSystem<Void>>() {})))
        .thenReturn(actorSystem);

    var hookRef = new AtomicReference<Thread>();
    var app = new ServerApp();
    app.registerShutdownHook(injector, hookRef::set);

    assertThat(hookRef.get()).isNotNull();
    assertThat(hookRef.get().getName()).isEqualTo("pekko-coordinated-shutdown");
  }

  @Test
  @DisplayName("registerShutdownHook ignores missing ActorSystem safely")
  void registerShutdownHook_whenActorSystemAbsent_doesNotThrow() {
    var injector = mock(Injector.class);
    when(injector.getInstance(Key.get(new TypeLiteral<ActorSystem<Void>>() {}))).thenReturn(null);

    var hookRef = new AtomicReference<Thread>();
    var app = new ServerApp();
    app.registerShutdownHook(injector, hookRef::set);

    assertThat(hookRef.get()).isNull();
  }
}
