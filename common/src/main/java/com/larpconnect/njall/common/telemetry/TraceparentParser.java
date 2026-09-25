package com.larpconnect.njall.common.telemetry;

import java.util.Optional;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/** Parser for standard W3C traceparent header strings. */
public final class TraceparentParser {

  private static final Pattern W3C_TRACEPARENT_PATTERN =
      Pattern.compile("^00-([0-9a-f]{32})-([0-9a-f]{16})-01$");

  private TraceparentParser() {}

  /**
   * Parses a raw W3C {@code traceparent} header string into an optional {@link TraceContext}.
   *
   * @param headerValue the raw header value
   * @return optional containing the parsed trace context, or empty if null or invalid
   */
  public static Optional<TraceContext> parse(@Nullable String headerValue) {
    if (headerValue == null) {
      return Optional.empty();
    }
    var matcher = W3C_TRACEPARENT_PATTERN.matcher(headerValue);
    if (!matcher.matches()) {
      return Optional.empty();
    }
    return Optional.of(new TraceContext(matcher.group(1), matcher.group(2)));
  }
}
