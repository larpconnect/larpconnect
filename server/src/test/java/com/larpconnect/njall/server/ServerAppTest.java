package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.larpconnect.njall.data.migration.DatabaseMigrator;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.pekko.actor.typed.ActorSystem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
final class ServerAppTest {

  @Mock private ActorSystem<Void> actorSystem;

  @Test
  @DisplayName("runWithArgs executes migration and returns 0 on success")
  void runWithArgs_whenMigrateSucceeds_returnsZero() {
    var migrator = mock(DatabaseMigrator.class);
    var injector = mock(Injector.class);
    when(injector.getInstance(DatabaseMigrator.class)).thenReturn(migrator);

    var app = new ServerApp(() -> injector, inj -> {});
    var exitCode = app.runWithArgs(new String[] {"--migrate"});

    assertThat(exitCode).isEqualTo(0);
    verify(migrator).migrate();
  }

  @Test
  @DisplayName("runWithArgs returns 1 when migration throws exception")
  void runWithArgs_whenMigrateFails_returnsOne() {
    var migrator = mock(DatabaseMigrator.class);
    doThrow(new RuntimeException("DB error")).when(migrator).migrate();
    var injector = mock(Injector.class);
    when(injector.getInstance(DatabaseMigrator.class)).thenReturn(migrator);

    var app = new ServerApp(() -> injector, inj -> {});
    var exitCode = app.runWithArgs(new String[] {"--migrate"});

    assertThat(exitCode).isEqualTo(1);
    verify(migrator).migrate();
  }

  @Test
  @DisplayName("runWithArgs invokes server launcher and returns null when --migrate is absent")
  void runWithArgs_whenMigrateAbsent_invokesServerLauncherAndReturnsNull() {
    var launchInvoked = new AtomicBoolean(false);
    var hookRegistered = new AtomicBoolean(false);
    var injector = mock(Injector.class);
    when(injector.getInstance(Key.get(new TypeLiteral<ActorSystem<Void>>() {})))
        .thenReturn(actorSystem);

    var app =
        new ServerApp(
            () -> injector, inj -> launchInvoked.set(true), hook -> hookRegistered.set(true));
    var exitCode = app.runWithArgs(new String[] {"--port", "8080"});

    assertThat(exitCode).isNull();
    assertThat(launchInvoked.get()).isTrue();
    assertThat(hookRegistered.get()).isTrue();
  }

  @Test
  @DisplayName("default constructor initializes valid ServerApp")
  void constructor_default_initializesApp() {
    var app = new ServerApp();
    assertThat(app).isNotNull();
    assertThat(app.parseArgs(new String[] {"--migrate"}).migrate()).isTrue();
  }
}
