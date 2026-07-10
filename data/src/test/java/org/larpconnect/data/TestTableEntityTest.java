package org.larpconnect.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link TestTableEntity} verifying entity attributes, equals, and hashCode. */
public final class TestTableEntityTest {
  @Test
  public void gettersAndSetters_workAsExpected() {
    UUID id = UUID.randomUUID();
    TestTableEntity entity = new TestTableEntity();
    entity.setId(id);
    entity.setName("Test Name");

    assertThat(entity.getId()).isEqualTo(id);
    assertThat(entity.getName()).isEqualTo("Test Name");
  }

  @Test
  public void parameterizedConstructor_initializesCorrectly() {
    UUID id = UUID.randomUUID();
    TestTableEntity entity = new TestTableEntity(id, "Param Name");

    assertThat(entity.getId()).isEqualTo(id);
    assertThat(entity.getName()).isEqualTo("Param Name");
  }

  @Test
  public void equalsAndHashCode_verifyContracts() {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();

    TestTableEntity entity1 = new TestTableEntity(id1, "Name A");
    TestTableEntity entity2 = new TestTableEntity(id1, "Name A");
    TestTableEntity entity3 = new TestTableEntity(id1, "Name B");
    TestTableEntity entity4 = new TestTableEntity(id2, "Name A");

    // Equals reflexive
    assertThat(entity1.equals(entity1)).isTrue();

    // Equals symmetric
    assertThat(entity1).isEqualTo(entity2);
    assertThat(entity2).isEqualTo(entity1);

    // Equals false cases
    assertThat(entity1).isNotEqualTo(entity3);
    assertThat(entity1).isNotEqualTo(entity4);
    assertThat(entity1).isNotEqualTo(null);
    assertThat(entity1).isNotEqualTo("some string");

    // HashCode contract
    assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
    assertThat(entity1.hashCode()).isNotEqualTo(entity4.hashCode());
  }
}
