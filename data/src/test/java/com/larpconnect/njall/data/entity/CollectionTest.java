package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class CollectionTest {

  @Test
  void gettersAndSetters_validInput_returnsExpected() {
    Collection entity = new Collection();

    Entity ownerVal = Mockito.mock(Entity.class);
    entity.setOwner(ownerVal);
    assertThat(entity.getOwner()).isEqualTo(ownerVal);
    String nameVal = "test";
    entity.setName(nameVal);
    assertThat(entity.getName()).isEqualTo(nameVal);
    UUID idVal = UUID.randomUUID();
    entity.setId(idVal);
    assertThat(entity.getId()).isEqualTo(idVal);
  }
}
