package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class IndividualTest {

  private Individual createInstance() throws Exception {
    if (java.lang.reflect.Modifier.isAbstract(Individual.class.getModifiers())) {
      return new Individual() {};
    }
    java.lang.reflect.Constructor<Individual> ctor = Individual.class.getDeclaredConstructor();
    ctor.setAccessible(true);
    return ctor.newInstance();
  }

  @Test
  void test_Name() throws Exception {
    Individual obj = createInstance();
    String val = "test";
    obj.setName(val);
    assertThat(obj.getName()).isEqualTo(val);
  }
}
