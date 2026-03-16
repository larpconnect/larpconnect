package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ServerMetadataTest {

  private ServerMetadata createInstance() throws Exception {
    if (java.lang.reflect.Modifier.isAbstract(ServerMetadata.class.getModifiers())) {
      return new ServerMetadata() {};
    }
    java.lang.reflect.Constructor<ServerMetadata> ctor =
        ServerMetadata.class.getDeclaredConstructor();
    ctor.setAccessible(true);
    return ctor.newInstance();
  }

  @Test
  void test_Name() throws Exception {
    ServerMetadata obj = createInstance();
    String val = "test";
    obj.setName(val);
    assertThat(obj.getName()).isEqualTo(val);
  }
}
