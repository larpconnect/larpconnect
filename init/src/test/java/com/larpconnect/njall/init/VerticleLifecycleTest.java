package com.larpconnect.njall.init;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.file.FileSystem;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

final class VerticleLifecycleTest {

  private Vertx mockVertx;
  private EventBus mockEventBus;
  private FileSystem mockFileSystem;
  private VertxProvider mockProvider;
  private Context mockContext;

  @BeforeEach
  void setUp() {
    mockVertx = mock(Vertx.class);
    mockEventBus = mock(EventBus.class);
    mockFileSystem = mock(FileSystem.class);
    mockProvider = mock(VertxProvider.class);
    mockContext = mock(Context.class);

    when(mockVertx.eventBus()).thenReturn(mockEventBus);
    when(mockVertx.fileSystem()).thenReturn(mockFileSystem);
    when(mockVertx.getOrCreateContext()).thenReturn(mockContext);

    // Ensure runOnContext executes immediately to avoid hangs in ConfigRetriever
    Answer<Void> runOnContextAnswer =
        new Answer<Void>() {
          @Override
          public Void answer(InvocationOnMock invocation) throws Throwable {
            Handler<Void> handler = invocation.getArgument(0);
            handler.handle(null);
            return null;
          }
        };

    doAnswer(runOnContextAnswer).when(mockContext).runOnContext(any());
    doAnswer(runOnContextAnswer).when(mockVertx).runOnContext(any());
  }

  @Test
  public void startUp_validConfig_success() throws Exception {
    var lifecycle = VerticleServices.create(Collections.emptyList());

    lifecycle.startAsync().awaitRunning(10, TimeUnit.SECONDS);
    assertThat(lifecycle.isRunning()).isTrue();

    lifecycle.stopAsync().awaitTerminated(10, TimeUnit.SECONDS);
    assertThat(lifecycle.isRunning()).isFalse();
  }

  @Test
  public void shutDown_closesVertx() {
    when(mockVertx.close()).thenReturn(Future.succeededFuture());
    when(mockProvider.get()).thenReturn(mockVertx);

    var lifecycle = new VerticleLifecycle(Collections.emptyList(), mockProvider);
    lifecycle.startAsync().awaitRunning(); // Will likely timeout on config but proceed due to catch
    lifecycle.stopAsync().awaitTerminated();

    verify(mockVertx).close();
  }

  @Test
  public void shutDown_getReturnsNull_doesNothing() {
    when(mockProvider.get())
        .thenReturn(mockVertx) // First call in startUp
        .thenReturn(null); // Second call in shutDown

    var lifecycle = new VerticleLifecycle(Collections.emptyList(), mockProvider);

    lifecycle.startAsync().awaitRunning();
    lifecycle.stopAsync().awaitTerminated();

    verify(mockVertx, never()).close();
  }

  @Test
  public void shutDown_closeFails_logsError() {
    when(mockVertx.close()).thenReturn(Future.failedFuture("failure"));
    when(mockProvider.get()).thenReturn(mockVertx);

    var lifecycle = new VerticleLifecycle(Collections.emptyList(), mockProvider);
    lifecycle.startAsync().awaitRunning();
    lifecycle.stopAsync().awaitTerminated();

    verify(mockVertx).close();
  }

  @Test
  public void deploy_delegatesToService() {
    when(mockVertx.deployVerticle(anyString())).thenReturn(Future.succeededFuture("id"));
    when(mockVertx.close()).thenReturn(Future.succeededFuture());
    when(mockProvider.get()).thenReturn(mockVertx);

    var lifecycle = new VerticleLifecycle(Collections.emptyList(), mockProvider);
    lifecycle.startAsync().awaitRunning();
    lifecycle.deploy(TestVerticle.class);
    lifecycle.stopAsync().awaitTerminated();

    verify(mockVertx).deployVerticle("guice:" + TestVerticle.class.getName());
  }

  @Test
  public void deploy_notStarted_throwsException() {
    var lifecycle = new VerticleLifecycle(Collections.emptyList());
    assertThatThrownBy(() -> lifecycle.deploy(TestVerticle.class))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("VerticleLifecycle not started");
  }

  static final class TestVerticle extends AbstractVerticle {}
}
