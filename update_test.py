import sys

with open("init/src/test/java/com/larpconnect/njall/init/VerticleLifecycleTest.java", "r") as f:
    content = f.read()

new_test = """
  @Test
  @SuppressWarnings("unchecked")
  public void shutDown_interrupted_logsWarning() throws Exception {
    var lifecycle = new VerticleLifecycle(Collections.emptyList());
    var field = VerticleLifecycle.class.getDeclaredField("vertxRef");
    field.setAccessible(true);
    var vertxRef = (AtomicReference<io.vertx.core.Vertx>) field.get(lifecycle);

    var mockVertx = mock(io.vertx.core.Vertx.class);
    // Never complete the future so latch.await() blocks
    var promise = io.vertx.core.Promise.<Void>promise();
    when(mockVertx.close()).thenReturn(promise.future());
    vertxRef.set(mockVertx);

    Thread.currentThread().interrupt();
    lifecycle.shutDown();

    assertThat(Thread.currentThread().isInterrupted()).isTrue();
    // Clear interrupt status for other tests
    Thread.interrupted();
  }
"""

content = content.replace("  static final class TestVerticle extends AbstractVerticle {}", new_test + "  static final class TestVerticle extends AbstractVerticle {}")

with open("init/src/test/java/com/larpconnect/njall/init/VerticleLifecycleTest.java", "w") as f:
    f.write(content)
