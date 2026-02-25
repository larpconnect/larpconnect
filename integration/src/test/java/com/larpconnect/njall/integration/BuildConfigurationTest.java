package com.larpconnect.njall.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import org.junit.jupiter.api.Test;

/** Tests to verify that the build configuration is correct. */
class BuildConfigurationTest {

  @Test
  void verifyCompilation_parametersFlagEnabled_preservesParameterNames() throws Exception {
    Method method =
        BuildConfigurationTest.class.getDeclaredMethod("exampleMethod", String.class, int.class);
    Parameter[] parameters = method.getParameters();

    assertThat(parameters[0].isNamePresent())
        .as("Parameter names must be preserved by compilation with -parameters flag")
        .isTrue();
    assertThat(parameters[0].getName()).isEqualTo("param1");
    assertThat(parameters[1].getName()).isEqualTo("param2");
  }

  @SuppressWarnings({"unused", "UnusedMethod", "UnusedVariable"})
  private void exampleMethod(String param1, int param2) {
    // No-op method for reflection testing
  }
}
