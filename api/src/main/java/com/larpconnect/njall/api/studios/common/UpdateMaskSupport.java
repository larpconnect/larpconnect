package com.larpconnect.njall.api.studios.common;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Immutable;
import java.util.Optional;

/** Shared AIP-134 {@code update_mask} parsing and field-selection helpers for PATCH actors. */
@Immutable
public final class UpdateMaskSupport {

  private static final Splitter MASK_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  private UpdateMaskSupport() {}

  /**
   * Parses a comma-separated {@code update_mask} parameter into normalized field names.
   *
   * @param rawUpdateMask Optional raw {@code update_mask} query or payload string.
   * @return Non-empty set of requested field paths, or empty when no field mask is active.
   */
  public static Optional<ImmutableSet<String>> parseUpdateMask(Optional<String> rawUpdateMask) {
    return rawUpdateMask
        .map(mask -> ImmutableSet.copyOf(MASK_SPLITTER.split(mask)))
        .filter(fields -> !fields.isEmpty());
  }

  /**
   * Determines whether a field should be updated given an optional field mask and request value.
   *
   * <p>When an update mask is present, the field is targeted if any of {@code fieldNames} appears
   * in the mask (allowing callers to accept both camelCase and snake_case aliases). When no mask is
   * present, the field is targeted only if {@code requestValue} is present.
   *
   * @param maskFields Parsed field mask from {@link #parseUpdateMask(Optional)}.
   * @param requestValue Optional field value supplied in the update request.
   * @param fieldNames Candidate field names matching this field in the mask.
   * @return {@code true} if the field should be applied from the request (or cleared if empty).
   */
  public static boolean isFieldTargeted(
      Optional<ImmutableSet<String>> maskFields, Optional<?> requestValue, String... fieldNames) {
    return maskFields.map(mask -> matchesAny(mask, fieldNames)).orElseGet(requestValue::isPresent);
  }

  /**
   * Determines whether a nullable field should be explicitly cleared by an active update mask.
   *
   * @param maskFields Parsed field mask from {@link #parseUpdateMask(Optional)}.
   * @param requestValue Optional field value supplied in the update request.
   * @param fieldNames Candidate field names matching this field in the mask.
   * @return {@code true} when an update mask targets this field and {@code requestValue} is empty.
   */
  public static boolean isFieldCleared(
      Optional<ImmutableSet<String>> maskFields, Optional<?> requestValue, String... fieldNames) {
    return requestValue.isEmpty() && isFieldTargeted(maskFields, requestValue, fieldNames);
  }

  /**
   * Resolves the updated value for a non-nullable field under AIP-134 {@code update_mask} rules.
   *
   * @param <T> Field value type.
   * @param requestValue Optional value from the PATCH request.
   * @param currentValue Existing persisted value.
   * @param maskFields Parsed field mask.
   * @param fieldNames Candidate field names matching this field in the mask.
   * @return Updated value when targeted and present; otherwise {@code currentValue}.
   */
  public static <T> T selectRequiredField(
      Optional<T> requestValue,
      T currentValue,
      Optional<ImmutableSet<String>> maskFields,
      String... fieldNames) {
    return isFieldTargeted(maskFields, requestValue, fieldNames)
        ? requestValue.orElse(currentValue)
        : currentValue;
  }

  /**
   * Resolves the updated value for a nullable optional field under AIP-134 {@code update_mask}
   * rules.
   *
   * <p>When an active update mask names this field and {@code requestValue} is empty, the field is
   * cleared to {@link Optional#empty()}.
   *
   * @param <T> Field value type.
   * @param requestValue Optional value from the PATCH request.
   * @param currentValue Existing optional persisted value.
   * @param maskFields Parsed field mask.
   * @param fieldNames Candidate field names matching this field in the mask.
   * @return Updated optional value (including {@link Optional#empty()} when explicitly cleared).
   */
  public static <T> Optional<T> selectOptionalField(
      Optional<T> requestValue,
      Optional<T> currentValue,
      Optional<ImmutableSet<String>> maskFields,
      String... fieldNames) {
    return isFieldTargeted(maskFields, requestValue, fieldNames) ? requestValue : currentValue;
  }

  private static boolean matchesAny(ImmutableSet<String> mask, String... fieldNames) {
    for (var fieldName : fieldNames) {
      if (mask.contains(fieldName)) {
        return true;
      }
    }
    return false;
  }
}
