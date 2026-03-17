package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.AbstractIdleService;
import com.larpconnect.njall.init.VerticleService;
import com.larpconnect.njall.init.VerticleServices;
import io.vertx.core.Verticle;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class MainExceptionTest {

  @Test
  public void run_throwsException_exits() {
    Runtime runtime = mock(Runtime.class);

    Main main = new Main(runtime, new ServerModule());

    try (MockedStatic<VerticleServices> verticleServices = mockStatic(VerticleServices.class)) {
      TestVerticleService lifecycle = new TestVerticleService();

      verticleServices.when(() -> VerticleServices.create(any())).thenReturn(lifecycle);

      assertThatThrownBy(main::run).isInstanceOf(AssertionError.class).hasMessage("Unreachable");

      verify(runtime).exit(1);
    }
  }

  private static final class TestVerticleService extends AbstractIdleService
      implements VerticleService {

    @Override
    public void deploy(Class<? extends Verticle> verticleClass) {
      throw new RuntimeException("Mocked failure");
    }

    @Override
    protected void startUp() throws Exception {}

    @Override
    protected void shutDown() throws Exception {}
  }
}
