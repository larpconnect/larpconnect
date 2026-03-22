package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class CollectionItemTest {

  @Test
  void gettersAndSetters_validInput_returnsExpected() {
    var entity = new CollectionItem();

    var idVal = UUID.randomUUID();
    entity.setId(idVal);
    assertThat(entity.getId()).isEqualTo(idVal);
    var collectionVal = mock(Collection.class);
    entity.setCollection(collectionVal);
    assertThat(entity.getCollection()).isEqualTo(collectionVal);
    OffsetDateTime addedOnVal = OffsetDateTime.now(java.time.ZoneId.systemDefault());
    entity.setAddedOn(addedOnVal);
    assertThat(entity.getAddedOn()).isEqualTo(addedOnVal);
    var refersToVal = mock(Entity.class);
    entity.setRefersTo(refersToVal);
    assertThat(entity.getRefersTo()).isEqualTo(refersToVal);
  }
}
