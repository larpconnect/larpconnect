package com.larpconnect.njall.api.studios.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class UpdateMaskSupportTest {

  @Test
  void parseUpdateMask_emptyOrBlank_returnsEmpty() {
    assertThat(UpdateMaskSupport.parseUpdateMask(Optional.empty())).isEmpty();
    assertThat(UpdateMaskSupport.parseUpdateMask(Optional.of(""))).isEmpty();
    assertThat(UpdateMaskSupport.parseUpdateMask(Optional.of(" , , "))).isEmpty();
  }

  @Test
  void parseUpdateMask_commaSeparatedFields_trimsAndDeduplicates() {
    var parsed = UpdateMaskSupport.parseUpdateMask(Optional.of(" summary , name , summary "));
    assertThat(parsed).contains(ImmutableSet.of("summary", "name"));
  }

  @Test
  void isFieldTargeted_withoutMask_followsRequestPresence() {
    var empty = Optional.<String>empty();

    assertThat(UpdateMaskSupport.isFieldTargeted(Optional.empty(), Optional.of("v"), "summary"))
        .isTrue();
    assertThat(UpdateMaskSupport.isFieldTargeted(Optional.empty(), empty, "summary")).isFalse();
  }

  @Test
  void isFieldTargeted_withMask_matchesAnyAlias() {
    var mask = Optional.of(ImmutableSet.of("start_time", "summary"));
    var empty = Optional.<String>empty();

    assertThat(UpdateMaskSupport.isFieldTargeted(mask, empty, "startTime", "start_time")).isTrue();
    assertThat(UpdateMaskSupport.isFieldTargeted(mask, Optional.of("v"), "name")).isFalse();
    assertThat(UpdateMaskSupport.isFieldTargeted(mask, Optional.of("v"))).isFalse();
  }

  @Test
  void isFieldCleared_requiresActiveMaskAndEmptyRequestValue() {
    var mask = Optional.of(ImmutableSet.of("summary"));
    var empty = Optional.<String>empty();

    assertThat(UpdateMaskSupport.isFieldCleared(Optional.empty(), empty, "summary")).isFalse();
    assertThat(UpdateMaskSupport.isFieldCleared(mask, empty, "location")).isFalse();
    assertThat(UpdateMaskSupport.isFieldCleared(mask, Optional.of("keep"), "summary")).isFalse();
    assertThat(UpdateMaskSupport.isFieldCleared(mask, empty, "summary")).isTrue();
  }

  @Test
  void selectRequiredField_appliesMaskAndFallbackRules() {
    var mask = Optional.of(ImmutableSet.of("name"));
    var empty = Optional.<String>empty();

    var noMaskUpdated =
        UpdateMaskSupport.selectRequiredField(Optional.of("New"), "Old", Optional.empty(), "name");
    var noMaskFallback =
        UpdateMaskSupport.selectRequiredField(empty, "Old", Optional.empty(), "name");
    var maskedUpdated =
        UpdateMaskSupport.selectRequiredField(Optional.of("New"), "Old", mask, "name");
    var maskedFallback = UpdateMaskSupport.selectRequiredField(empty, "Old", mask, "name");
    var omitted =
        UpdateMaskSupport.selectRequiredField(Optional.of("Ignored"), "Old", mask, "summary");

    assertThat(noMaskUpdated).isEqualTo("New");
    assertThat(noMaskFallback).isEqualTo("Old");
    assertThat(maskedUpdated).isEqualTo("New");
    assertThat(maskedFallback).isEqualTo("Old");
    assertThat(omitted).isEqualTo("Old");
  }

  @Test
  void selectOptionalField_supportsPatchUpdateMaskAndExplicitClear() {
    var mask = Optional.of(ImmutableSet.of("start_time", "summary"));
    var empty = Optional.<String>empty();
    var old = Optional.of("Old");

    var noMaskUpdated =
        UpdateMaskSupport.selectOptionalField(Optional.of("New"), old, Optional.empty(), "summary");
    var noMaskKeep = UpdateMaskSupport.selectOptionalField(empty, old, Optional.empty(), "summary");
    var maskedCleared = UpdateMaskSupport.selectOptionalField(empty, old, mask, "summary");
    var maskedAliasUpdated =
        UpdateMaskSupport.selectOptionalField(
            Optional.of("NewStart"), old, mask, "startTime", "start_time");
    var omitted =
        UpdateMaskSupport.selectOptionalField(Optional.of("Ignored"), old, mask, "location");

    assertThat(noMaskUpdated).contains("New");
    assertThat(noMaskKeep).contains("Old");
    assertThat(maskedCleared).isEmpty();
    assertThat(maskedAliasUpdated).contains("NewStart");
    assertThat(omitted).contains("Old");
  }
}
