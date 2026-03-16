package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ContactInfoTest {

  private ContactInfo createInstance() throws Exception {
    if (java.lang.reflect.Modifier.isAbstract(ContactInfo.class.getModifiers())) {
      return new ContactInfo() {};
    }
    java.lang.reflect.Constructor<ContactInfo> ctor = ContactInfo.class.getDeclaredConstructor();
    ctor.setAccessible(true);
    return ctor.newInstance();
  }

  @Test
  void test_Id() throws Exception {
    ContactInfo obj = createInstance();
    UUID val = java.util.UUID.randomUUID();
    obj.setId(val);
    assertThat(obj.getId()).isEqualTo(val);
  }

  @Test
  void test_Owner() throws Exception {
    ContactInfo obj = createInstance();
    Entity val = org.mockito.Mockito.mock(Entity.class);
    obj.setOwner(val);
    assertThat(obj.getOwner()).isEqualTo(val);
  }

  @Test
  void test_Ordering() throws Exception {
    ContactInfo obj = createInstance();
    int val = 1;
    obj.setOrdering(val);
    assertThat(obj.getOrdering()).isEqualTo(val);
  }

  @Test
  void test_ContactType() throws Exception {
    ContactInfo obj = createInstance();
    ContactType val = org.mockito.Mockito.mock(ContactType.class);
    obj.setContactType(val);
    assertThat(obj.getContactType()).isEqualTo(val);
  }

  @Test
  void test_Contact() throws Exception {
    ContactInfo obj = createInstance();
    String val = "test";
    obj.setContact(val);
    assertThat(obj.getContact()).isEqualTo(val);
  }

  @Test
  void test_isActive() throws Exception {
    ContactInfo obj = createInstance();
    boolean val = true;
    obj.setActive(val);
    assertThat(obj.isActive()).isEqualTo(val);
  }
}
