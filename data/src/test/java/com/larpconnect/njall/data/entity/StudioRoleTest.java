package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class StudioRoleTest {

  private StudioRole createInstance() throws Exception {
    if (java.lang.reflect.Modifier.isAbstract(StudioRole.class.getModifiers())) {
      return new StudioRole() {};
    }
    java.lang.reflect.Constructor<StudioRole> ctor = StudioRole.class.getDeclaredConstructor();
    ctor.setAccessible(true);
    return ctor.newInstance();
  }

  private StudioRole.StudioRoleId createStudioRoleId() throws Exception {
    java.lang.reflect.Constructor<StudioRole.StudioRoleId> ctor =
        StudioRole.StudioRoleId.class.getDeclaredConstructor();
    ctor.setAccessible(true);
    return ctor.newInstance();
  }

  @Test
  void test_Studio() throws Exception {
    StudioRole obj = createInstance();
    UUID val = java.util.UUID.randomUUID();
    obj.setStudio(val);
    assertThat(obj.getStudio()).isEqualTo(val);
  }

  @Test
  void test_Role() throws Exception {
    StudioRole obj = createInstance();
    UUID val = java.util.UUID.randomUUID();
    obj.setRole(val);
    assertThat(obj.getRole()).isEqualTo(val);
  }

  @Test
  void test_Individual() throws Exception {
    StudioRole obj = createInstance();
    UUID val = java.util.UUID.randomUUID();
    obj.setIndividual(val);
    assertThat(obj.getIndividual()).isEqualTo(val);
  }

  @Test
  void test_Studio() throws Exception {
    StudioRole obj = createInstance();
    UUID val = java.util.UUID.randomUUID();
    obj.setStudio(val);
    assertThat(obj.getStudio()).isEqualTo(val);
  }

  @Test
  void test_Role() throws Exception {
    StudioRole obj = createInstance();
    UUID val = java.util.UUID.randomUUID();
    obj.setRole(val);
    assertThat(obj.getRole()).isEqualTo(val);
  }

  @Test
  void test_Individual() throws Exception {
    StudioRole obj = createInstance();
    UUID val = java.util.UUID.randomUUID();
    obj.setIndividual(val);
    assertThat(obj.getIndividual()).isEqualTo(val);
  }

  @Test
  void test_studioRoleId_equals() throws Exception {
    StudioRole.StudioRoleId id1 = createStudioRoleId();
    StudioRole.StudioRoleId id2 = createStudioRoleId();
    java.util.UUID u = java.util.UUID.randomUUID();
    id1.setStudio(u);
    id2.setStudio(u);
    id1.setRole(u);
    id2.setRole(u);
    id1.setIndividual(u);
    id2.setIndividual(u);
    assertThat(id1.equals(id2)).isTrue();
    assertThat(id1.hashCode()).isEqualTo(id2.hashCode());

    id2.setStudio(java.util.UUID.randomUUID());
    assertThat(id1.equals(id2)).isFalse();

    assertThat(id1.equals(null)).isFalse();
    assertThat(id1.equals(new Object())).isFalse();
    assertThat(id1.equals(id1)).isTrue();
  }
}
