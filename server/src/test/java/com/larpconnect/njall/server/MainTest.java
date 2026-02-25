package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.util.concurrent.Service.State;
import com.larpconnect.njall.init.VerticleService;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

class MainTest {

  @Test
  void main_startsAndStopsServerSuccessfully() {
    Main main = new Main();
    VerticleService service = main.run();

    assertThat(service.state()).isEqualTo(State.RUNNING);

    main.shutdown(service);

    assertThat(service.state()).isEqualTo(State.TERMINATED);
  }

  @Test
  void shutdown_handlesExceptions() {
    Main main = new Main();

    // Test TimeoutException
    VerticleService timeoutService =
        (VerticleService)
            Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {VerticleService.class},
                (proxy, method, args) -> {
                  if (method.getName().equals("stopAsync")) {
                    return proxy;
                  }
                  if (method.getName().equals("awaitTerminated")) {
                    throw new TimeoutException();
                  }
                  return null;
                });

    main.shutdown(timeoutService);

    // Test RuntimeException
    VerticleService runtimeService =
        (VerticleService)
            Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {VerticleService.class},
                (proxy, method, args) -> {
                  if (method.getName().equals("stopAsync")) {
                    return proxy;
                  }
                  if (method.getName().equals("awaitTerminated")) {
                    throw new RuntimeException();
                  }
                  return null;
                });

    main.shutdown(runtimeService);
  }
}
