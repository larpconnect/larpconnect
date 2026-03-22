package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class CollectionTest {

  @Test
  void gettersAndSetters_validInput_returnsExpected() {
    var entity = new Collection();

    var ownerVal = mock(Entity.class);
    entity.setOwner(ownerVal);
    assertThat(entity.getOwner()).isEqualTo(ownerVal);
    String nameVal = "test";
    entity.setName(nameVal);
    assertThat(entity.getName()).isEqualTo(nameVal);
    var idVal = UUID.randomUUID();
    entity.setId(idVal);
    assertThat(entity.getId()).isEqualTo(idVal);
  }
}
