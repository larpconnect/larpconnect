package com.larpconnect.njall.api.studios.inbox;

import static java.util.Arrays.stream;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.Immutable;
import com.google.inject.Inject;
import com.google.mu.util.Substring;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** Default RFC 9530 implementation of {@link ContentDigestVerifier}. */
final class DefaultContentDigestVerifier implements ContentDigestVerifier {

  private static final Splitter HEADER_SPLITTER = Splitter.on(',').trimResults();
  private static final Pattern ALGORITHM_TOKEN_PATTERN = Pattern.compile("[a-z*][a-z0-9_.*-]*");
  private static final Substring.Pattern EQ_COLON = Substring.first("=:");
  private static final Substring.Pattern CLOSING_COLON = Substring.first(':');

  @Inject
  DefaultContentDigestVerifier() {}

  @Override
  public Optional<String> verify(byte[] payloadBytes, Optional<String> headerValue) {
    return headerValue
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .flatMap(value -> parseAndVerify(payloadBytes, value));
  }

  private Optional<String> parseAndVerify(byte[] payloadBytes, String header) {
    String firstUnsupported = null;
    int supportedMatches = 0;

    for (String element : HEADER_SPLITTER.split(header)) {
      var parsed = parseDictionaryMember(element);
      switch (parsed) {
        case MemberParseResult.Invalid(var errorMessage) -> {
          return Optional.of(errorMessage);
        }
        case MemberParseResult.Valid(var algorithm, var base64Digest) -> {
          var digestAlgorithmOpt = DigestAlgorithm.fromHeaderName(algorithm);
          if (digestAlgorithmOpt.isEmpty()) {
            if (firstUnsupported == null) {
              firstUnsupported = algorithm;
            }
            continue;
          }
          var verifyResult =
              verifyDigest(payloadBytes, digestAlgorithmOpt.orElseThrow(), base64Digest);
          if (verifyResult.isPresent()) {
            return verifyResult;
          }
          supportedMatches++;
        }
      }
    }

    if (supportedMatches > 0) {
      return Optional.empty();
    }
    return Optional.of("Unsupported digest algorithm: " + firstUnsupported);
  }

  private static MemberParseResult parseDictionaryMember(String element) {
    var delimMatch = EQ_COLON.in(element);
    if (delimMatch.isEmpty()) {
      return createInvalidMember("Invalid Content-Digest format: missing byte delimiter '=:'");
    }
    var eqMatch = delimMatch.orElseThrow();
    var endMatch = CLOSING_COLON.in(eqMatch.after());
    if (endMatch.isEmpty()) {
      return createInvalidMember("Invalid Content-Digest format: unclosed byte sequence delimiter");
    }
    var closeMatch = endMatch.orElseThrow();
    String trailing = closeMatch.after().trim();
    if (!trailing.isEmpty() && !trailing.startsWith(";")) {
      return createInvalidMember(
          "Invalid Content-Digest format: unexpected characters after closing ':'");
    }

    String algorithm = eqMatch.before().toLowerCase(Locale.ROOT);
    if (!ALGORITHM_TOKEN_PATTERN.matcher(algorithm).matches()) {
      return createInvalidMember("Invalid Content-Digest format: invalid algorithm token");
    }

    return createValidMember(algorithm, closeMatch.before().trim());
  }

  private Optional<String> verifyDigest(
      byte[] payloadBytes, DigestAlgorithm digestAlgorithm, String base64Digest) {
    byte[] expectedDigest;
    try {
      expectedDigest = Base64.getUrlDecoder().decode(base64Digest);
    } catch (IllegalArgumentException ex) {
      return Optional.of("Invalid base64 payload in Content-Digest header");
    }

    return compareDigest(payloadBytes, digestAlgorithm, expectedDigest);
  }

  private Optional<String> compareDigest(
      byte[] payloadBytes, DigestAlgorithm digestAlgorithm, byte[] expectedDigest) {
    try {
      MessageDigest md = MessageDigest.getInstance(digestAlgorithm.jvmName());
      byte[] actualDigest = md.digest(payloadBytes);
      if (!MessageDigest.isEqual(actualDigest, expectedDigest)) {
        return Optional.of(
            "Content digest mismatch for algorithm: " + digestAlgorithm.headerName());
      }
    } catch (NoSuchAlgorithmException ex) {
      return Optional.of("Digest algorithm unavailable in runtime: " + digestAlgorithm.jvmName());
    }
    return Optional.empty();
  }

  @Override
  public String computeDigest(byte[] payloadBytes, String algorithm) {
    DigestAlgorithm digestAlgorithm =
        DigestAlgorithm.fromHeaderName(algorithm)
            .orElseThrow(() -> createUnsupportedAlgorithmException(algorithm));

    try {
      MessageDigest md = MessageDigest.getInstance(digestAlgorithm.jvmName());
      byte[] digest = md.digest(payloadBytes);
      String base64 = Base64.getUrlEncoder().encodeToString(digest);
      return digestAlgorithm.headerName() + "=:" + base64 + ":";
    } catch (NoSuchAlgorithmException ex) {
      throw createAlgorithmUnavailableException(digestAlgorithm.jvmName(), ex);
    }
  }

  private static MemberParseResult.Valid createValidMember(String algorithm, String base64Digest) {
    return new MemberParseResult.Valid(algorithm, base64Digest);
  }

  private static MemberParseResult.Invalid createInvalidMember(String errorMessage) {
    return new MemberParseResult.Invalid(errorMessage);
  }

  private static IllegalArgumentException createUnsupportedAlgorithmException(String algorithm) {
    return new IllegalArgumentException("Unsupported algorithm: " + algorithm);
  }

  private static IllegalStateException createAlgorithmUnavailableException(
      String jvmName, NoSuchAlgorithmException cause) {
    return new IllegalStateException("Algorithm unavailable: " + jvmName, cause);
  }

  private sealed interface MemberParseResult {
    @Immutable
    record Valid(String algorithm, String base64Digest) implements MemberParseResult {}

    @Immutable
    record Invalid(String errorMessage) implements MemberParseResult {}
  }

  private enum DigestAlgorithm {
    SHA_256("sha-256", "SHA-256"),
    SHA3_256("sha3-256", "SHA3-256");

    private static final ImmutableMap<String, DigestAlgorithm> BY_HEADER_NAME =
        stream(values()).collect(ImmutableMap.toImmutableMap(DigestAlgorithm::headerName, a -> a));

    private final String headerName;
    private final String jvmName;

    DigestAlgorithm(String headerName, String jvmName) {
      this.headerName = headerName;
      this.jvmName = jvmName;
    }

    String headerName() {
      return headerName;
    }

    String jvmName() {
      return jvmName;
    }

    static Optional<DigestAlgorithm> fromHeaderName(String name) {
      return Optional.ofNullable(BY_HEADER_NAME.get(name.toLowerCase(Locale.ROOT)));
    }
  }
}
