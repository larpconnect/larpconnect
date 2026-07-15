package org.larpconnect.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link TestTable} verifying entity attributes, equals, and hashCode. */
public final class TestTableTest {
  private static final UUID ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

  @Test
  public void gettersAndSetters_workAsExpected() {
    TestTable entity = new TestTable();
    entity.setId(ID_1);
    entity.setName("Test Name");

    assertThat(entity.getId()).isEqualTo(ID_1);
    assertThat(entity.getName()).isEqualTo("Test Name");
  }

  @Test
  public void parameterizedConstructor_initializesCorrectly() {
    TestTable entity = new TestTable(ID_1, "Param Name");

    assertThat(entity.getId()).isEqualTo(ID_1);
    assertThat(entity.getName()).isEqualTo("Param Name");
  }

  @Test
  public void equalsAndHashCode_verifyContracts() {
    TestTable entity1 = new TestTable(ID_1, "Name A");
    TestTable entity2 = new TestTable(ID_1, "Name A");
    TestTable entity3 = new TestTable(ID_1, "Name B");
    TestTable entity4 = new TestTable(ID_2, "Name A");

    // Equals reflexive
    assertThat(entity1.equals(entity1)).isTrue();

    // Equals symmetric
    assertThat(entity1).isEqualTo(entity2);
    assertThat(entity2).isEqualTo(entity1);

    // Equals evaluates only primary key (ID)
    assertThat(entity1).isEqualTo(entity3);
    assertThat(entity3).isEqualTo(entity1);

    // Equals false cases
    assertThat(entity1).isNotEqualTo(entity4);
    assertThat(entity1).isNotEqualTo(null);
    assertThat(entity1).isNotEqualTo("some string");

    // HashCode contract
    assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
    assertThat(entity1.hashCode()).isEqualTo(entity3.hashCode());
    assertThat(entity1.hashCode()).isNotEqualTo(entity4.hashCode());
  }
}
