package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ServerMetadataTest {
  private ServerMetadata createInstance() {
    return new ServerMetadata();
  }

  @Test
  void test_Name() {
    ServerMetadata obj = createInstance();
    String val = "test";
    obj.setName(val);
    assertThat(obj.getName()).isEqualTo(val);
  }
}
