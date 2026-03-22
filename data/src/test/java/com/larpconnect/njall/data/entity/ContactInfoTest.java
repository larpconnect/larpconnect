package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class ContactInfoTest {

  @Test
  void gettersAndSetters_validInput_returnsExpected() {
    ContactInfo entity = new ContactInfo();

    UUID idVal = UUID.randomUUID();
    entity.setId(idVal);
    assertThat(entity.getId()).isEqualTo(idVal);
    Entity ownerVal = mock(Entity.class);
    entity.setOwner(ownerVal);
    assertThat(entity.getOwner()).isEqualTo(ownerVal);
    Integer orderingVal = 1;
    entity.setOrdering(orderingVal);
    assertThat(entity.getOrdering()).isEqualTo(orderingVal);
    ContactInfo.ContactType contactTypeVal = ContactInfo.ContactType.email;
    entity.setContactType(contactTypeVal);
    assertThat(entity.getContactType()).isEqualTo(contactTypeVal);
    String contactVal = "test";
    entity.setContact(contactVal);
    assertThat(entity.getContact()).isEqualTo(contactVal);
    Boolean activeVal = true;
    entity.setActive(activeVal);
    assertThat(entity.getActive()).isEqualTo(activeVal);
  }
}
