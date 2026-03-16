package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ContactInfoTest {
  private ContactInfo createInstance() {
    return new ContactInfo();
  }

  @Test
  void test_Id() {
    ContactInfo obj = createInstance();
    UUID val = java.util.UUID.randomUUID();
    obj.setId(val);
    assertThat(obj.getId()).isEqualTo(val);
  }

  @Test
  void test_Owner() {
    ContactInfo obj = createInstance();
    Entity val = org.mockito.Mockito.mock(Entity.class);
    obj.setOwner(val);
    assertThat(obj.getOwner()).isEqualTo(val);
  }

  @Test
  void test_Ordering() {
    ContactInfo obj = createInstance();
    int val = 1;
    obj.setOrdering(val);
    assertThat(obj.getOrdering()).isEqualTo(val);
  }

  @Test
  void test_ContactType() {
    ContactInfo obj = createInstance();
    ContactType val = org.mockito.Mockito.mock(ContactType.class);
    obj.setContactType(val);
    assertThat(obj.getContactType()).isEqualTo(val);
  }

  @Test
  void test_Contact() {
    ContactInfo obj = createInstance();
    String val = "test";
    obj.setContact(val);
    assertThat(obj.getContact()).isEqualTo(val);
  }

  @Test
  void test_isActive() {
    ContactInfo obj = createInstance();
    boolean val = true;
    obj.setActive(val);
    assertThat(obj.isActive()).isEqualTo(val);
  }
}
