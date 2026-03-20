package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class CollectionItemTest {

  @Test
  void gettersAndSetters_validInput_returnsExpected() {
    CollectionItem entity = new CollectionItem();

    UUID idVal = UUID.randomUUID();
    entity.setId(idVal);
    assertThat(entity.getId()).isEqualTo(idVal);
    Collection collectionVal = Mockito.mock(Collection.class);
    entity.setCollection(collectionVal);
    assertThat(entity.getCollection()).isEqualTo(collectionVal);
    OffsetDateTime addedOnVal = OffsetDateTime.now(java.time.ZoneId.systemDefault());
    entity.setAddedOn(addedOnVal);
    assertThat(entity.getAddedOn()).isEqualTo(addedOnVal);
    Entity refersToVal = Mockito.mock(Entity.class);
    entity.setRefersTo(refersToVal);
    assertThat(entity.getRefersTo()).isEqualTo(refersToVal);
  }
}
