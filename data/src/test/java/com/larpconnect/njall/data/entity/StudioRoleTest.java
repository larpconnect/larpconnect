package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ObjectStreamClass;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class StudioRoleTest {

  @Test
  void gettersAndSetters_validInput_returnsExpected() {
    StudioRole entity = new StudioRole();

    Studio studioVal = Mockito.mock(Studio.class);
    entity.setStudio(studioVal);
    assertThat(entity.getStudio()).isEqualTo(studioVal);
    Role roleVal = Mockito.mock(Role.class);
    entity.setRole(roleVal);
    assertThat(entity.getRole()).isEqualTo(roleVal);
    Individual individualVal = Mockito.mock(Individual.class);
    entity.setIndividual(individualVal);
    assertThat(entity.getIndividual()).isEqualTo(individualVal);
  }

  @Test
  void equalsAndHashCode_validInput_returnsExpected() {
    UUID studio = UUID.randomUUID();
    UUID role = UUID.randomUUID();
    UUID individual = UUID.randomUUID();

    StudioRole.StudioRoleId id1 = new StudioRole.StudioRoleId();
    StudioRole.StudioRoleId id2 = new StudioRole.StudioRoleId(studio, role, individual);
    StudioRole.StudioRoleId id3 = new StudioRole.StudioRoleId(studio, role, individual);
    StudioRole.StudioRoleId id4 = new StudioRole.StudioRoleId(UUID.randomUUID(), role, individual);
    StudioRole.StudioRoleId id5 =
        new StudioRole.StudioRoleId(studio, UUID.randomUUID(), individual);
    StudioRole.StudioRoleId id6 = new StudioRole.StudioRoleId(studio, role, UUID.randomUUID());
    StudioRole.StudioRoleId id7 = new StudioRole.StudioRoleId(null, role, individual);
    StudioRole.StudioRoleId id8 = new StudioRole.StudioRoleId(studio, null, individual);
    StudioRole.StudioRoleId id9 = new StudioRole.StudioRoleId(studio, role, null);
    StudioRole.StudioRoleId id10 = new StudioRole.StudioRoleId();
    StudioRole.StudioRoleId id11 = new StudioRole.StudioRoleId(studio, role, individual);
    StudioRole.StudioRoleId id12 = new StudioRole.StudioRoleId(null, null, null);

    assertThat(id1).isNotEqualTo(id2);
    assertThat(id1).isEqualTo(id10);
    assertThat(id1).isNotEqualTo(new Actor()); // Test against different class type
    assertThat(id2).isEqualTo(id3);
    assertThat(id2).isEqualTo(id11); // Ensure true == true
    assertThat(id2).isNotEqualTo(id4);
    assertThat(id2).isNotEqualTo(id5);
    assertThat(id2).isNotEqualTo(id6);
    assertThat(id2).isNotEqualTo(id7);
    assertThat(id2).isNotEqualTo(id8);
    assertThat(id2).isNotEqualTo(id9);
    assertThat(id2).isNotEqualTo(id12);
    assertThat(id7).isNotEqualTo(id2);
    assertThat(id8).isNotEqualTo(id2);
    assertThat(id9).isNotEqualTo(id2);
    assertThat(id12).isNotEqualTo(id2);
    assertThat(id2).isNotEqualTo(null);
    assertThat(id2).isNotEqualTo(new Object());

    // Explicitly hit identical self check
    assertThat(id2.equals(id2)).isTrue();

    assertThat(id2.hashCode()).isEqualTo(id3.hashCode());
    assertThat(id2.hashCode()).isNotEqualTo(id4.hashCode());
  }

  @Test
  void serialVersionUID_validInput_returnsExpected() {
    long serialVersionUID =
        ObjectStreamClass.lookup(StudioRole.StudioRoleId.class).getSerialVersionUID();
    assertThat(serialVersionUID).isEqualTo(1L);
  }
}
