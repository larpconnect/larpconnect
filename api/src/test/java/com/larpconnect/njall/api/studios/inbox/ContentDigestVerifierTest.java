package com.larpconnect.njall.api.studios.inbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

final class ContentDigestVerifierTest {

  private ContentDigestVerifier verifier;

  @BeforeEach
  void setUp() {
    verifier = new DefaultContentDigestVerifier();
  }

  @Test
  @DisplayName("Empty or absent Content-Digest header is accepted as valid")
  void verify_emptyOrAbsent_isValid() {
    byte[] payload = "{}".getBytes(StandardCharsets.UTF_8);

    assertThat(verifier.verify(payload, Optional.empty())).isEmpty();
    assertThat(verifier.verify(payload, Optional.of(""))).isEmpty();
    assertThat(verifier.verify(payload, Optional.of("   "))).isEmpty();
  }

  @Test
  @DisplayName("Valid sha-256 Content-Digest header matching payload succeeds")
  void verify_validSha256_succeeds() throws Exception {
    byte[] payload = "{\"type\":\"Create\"}".getBytes(StandardCharsets.UTF_8);
    byte[] hash = MessageDigest.getInstance("SHA-256").digest(payload);
    String header = "sha-256=:" + Base64.getUrlEncoder().encodeToString(hash) + ":";

    assertThat(verifier.verify(payload, Optional.of(header))).isEmpty();
  }

  @Test
  @DisplayName("Valid sha3-256 Content-Digest header matching payload succeeds")
  void verify_validSha3_256_succeeds() throws Exception {
    byte[] payload = "{\"type\":\"Offer\"}".getBytes(StandardCharsets.UTF_8);
    byte[] hash = MessageDigest.getInstance("SHA3-256").digest(payload);
    String header = "sha3-256=:" + Base64.getUrlEncoder().encodeToString(hash) + ":";

    assertThat(verifier.verify(payload, Optional.of(header))).isEmpty();
  }

  @Test
  @DisplayName("Multi-value header with preceding unsupported algorithm succeeds on valid sha-256")
  void verify_multiValueWithUnsupportedPreceding_succeedsOnValidSupported() throws Exception {
    byte[] payload = "{\"type\":\"Accept\"}".getBytes(StandardCharsets.UTF_8);
    byte[] hash = MessageDigest.getInstance("SHA-256").digest(payload);
    String validSha256 = "sha-256=:" + Base64.getUrlEncoder().encodeToString(hash) + ":";
    String multiHeader = "sha-512=:unknown-or-unsupported-hash:, " + validSha256;

    assertThat(verifier.verify(payload, Optional.of(multiHeader))).isEmpty();
  }

  @Test
  @DisplayName("Multi-value dictionary header with mismatched supported algorithm fails")
  void verify_multiValueWithMismatchedSupported_returnsError() {
    byte[] payload = "{\"type\":\"Accept\"}".getBytes(StandardCharsets.UTF_8);
    String multiHeader = "sha-512=:unknown-hash:, sha-256=:d3JvbmctaGFzaA==:";

    var result = verifier.verify(payload, Optional.of(multiHeader));
    assertThat(result).isPresent();
    assertThat(result.orElseThrow()).contains("Content digest mismatch for algorithm: sha-256");
  }

  @Test
  @DisplayName("Multi-value dictionary header where all algorithms are unsupported returns error")
  void verify_multiValueAllUnsupported_returnsError() {
    byte[] payload = "{\"type\":\"Accept\"}".getBytes(StandardCharsets.UTF_8);
    String multiHeader = "sha-512=:unknown-hash:, md5=:1B2M2Y8AsgTpgAmY7PhCfg==:";

    var result = verifier.verify(payload, Optional.of(multiHeader));
    assertThat(result).isPresent();
    assertThat(result.orElseThrow()).contains("Unsupported digest algorithm: sha-512");
  }

  @Test
  @DisplayName("Mismatched sha-256 digest returns content digest mismatch error")
  void verify_mismatchedDigest_returnsError() {
    byte[] payload = "{\"type\":\"Create\"}".getBytes(StandardCharsets.UTF_8);
    String fakeDigest =
        Base64.getUrlEncoder()
            .encodeToString("wrong-hash-bytes-1234567890".getBytes(StandardCharsets.UTF_8));
    String header = "sha-256=:" + fakeDigest + ":";

    var result = verifier.verify(payload, Optional.of(header));
    assertThat(result).isPresent();
    assertThat(result.orElseThrow()).contains("Content digest mismatch");
  }

  @Test
  @DisplayName("Unsupported digest algorithm returns unsupported algorithm error")
  void verify_unsupportedAlgorithm_returnsError() {
    byte[] payload = "{}".getBytes(StandardCharsets.UTF_8);
    String header = "md5=:1B2M2Y8AsgTpgAmY7PhCfg==:";

    var result = verifier.verify(payload, Optional.of(header));
    assertThat(result).isPresent();
    assertThat(result.orElseThrow()).contains("Unsupported digest algorithm: md5");
  }

  @Test
  @DisplayName("Malformed delimiter without '=:' returns format error")
  void verify_malformedHeaderMissingDelimiter_returnsError() {
    byte[] payload = "{}".getBytes(StandardCharsets.UTF_8);
    String header = "sha-256=abc1234:";

    var result = verifier.verify(payload, Optional.of(header));
    assertThat(result).isPresent();
    assertThat(result.orElseThrow()).contains("missing byte delimiter");
  }

  @Test
  @DisplayName("Malformed header with unclosed colon delimiter returns format error")
  void verify_malformedHeaderUnclosedDelimiter_returnsError() {
    byte[] payload = "{}".getBytes(StandardCharsets.UTF_8);
    String header = "sha-256=:abc1234";

    var result = verifier.verify(payload, Optional.of(header));
    assertThat(result).isPresent();
    assertThat(result.orElseThrow()).contains("unclosed byte sequence delimiter");
  }

  @Test
  @DisplayName("Malformed base64 characters inside delimiter returns format error")
  void verify_malformedBase64_returnsError() {
    byte[] payload = "{}".getBytes(StandardCharsets.UTF_8);
    String header = "sha-256=:???invalid-base64???:";

    var result = verifier.verify(payload, Optional.of(header));
    assertThat(result).isPresent();
    assertThat(result.orElseThrow()).contains("Invalid base64 payload");
  }

  @Test
  @DisplayName("RFC 8941 parameters after closing ':' are accepted")
  void verify_rfc8941ParametersAfterClosingColon_succeeds() {
    byte[] payload = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
    String valid = verifier.computeDigest(payload, "sha-256");

    assertThat(verifier.verify(payload, Optional.of(valid + ";q=1.0"))).isEmpty();
  }

  @Test
  @DisplayName("Unexpected trailing characters or empty dictionary member after ',' are rejected")
  void verify_trailingGarbageOrDanglingComma_returnsError() {
    byte[] payload = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
    String valid = verifier.computeDigest(payload, "sha-256");

    assertThat(verifier.verify(payload, Optional.of(valid + "GARBAGE")))
        .isPresent()
        .hasValueSatisfying(msg -> assertThat(msg).contains("unexpected characters after closing"));

    assertThat(verifier.verify(payload, Optional.of(valid + ", ")))
        .isPresent()
        .hasValueSatisfying(msg -> assertThat(msg).contains("missing byte delimiter"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "=:d3JvbmctaGFzaA==:",
        "1invalid=:d3JvbmctaGFzaA==:",
        "sha 256=:d3JvbmctaGFzaA==:",
        "sha-256 =:d3JvbmctaGFzaA==:"
      })
  @DisplayName("Invalid RFC 8941 algorithm tokens are rejected")
  void verify_invalidAlgorithmTokens_returnsError(String invalidMember) {
    byte[] payload = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
    String valid = verifier.computeDigest(payload, "sha-256");

    assertThat(verifier.verify(payload, Optional.of(invalidMember + ", " + valid)))
        .isPresent()
        .hasValueSatisfying(msg -> assertThat(msg).contains("invalid algorithm token"));
  }

  @Test
  @DisplayName("computeDigest computes valid RFC 9530 header strings and rejects unsupported")
  void computeDigest_supportedAlgorithms_producesValidHeaders() {
    byte[] payload = "{\"test\":true}".getBytes(StandardCharsets.UTF_8);

    String sha256 = verifier.computeDigest(payload, "sha-256");
    assertThat(sha256).startsWith("sha-256=:");
    assertThat(verifier.verify(payload, Optional.of(sha256))).isEmpty();

    String sha3 = verifier.computeDigest(payload, "sha3-256");
    assertThat(sha3).startsWith("sha3-256=:");
    assertThat(verifier.verify(payload, Optional.of(sha3))).isEmpty();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> verifier.computeDigest(payload, "md5"))
        .withMessageContaining("Unsupported algorithm: md5");
  }

  @Test
  @DisplayName(
      "URL-safe Base64 encoding is emitted and decoded while standard '+'/'/' Base64 is rejected")
  void verify_urlSafeAlphabetRequired_acceptsUrlSafeAndRejectsStandardBase64() {
    byte[] emptyPayload = new byte[0];
    String urlSafeHeader = "sha-256=:47DEQpj8HBSa-_TImW-5JCeuQeRkm5NMpJWZG3hSuFU=:";
    String standardHeader = "sha-256=:47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=:";

    assertThat(verifier.computeDigest(emptyPayload, "sha-256")).isEqualTo(urlSafeHeader);
    assertThat(verifier.verify(emptyPayload, Optional.of(urlSafeHeader))).isEmpty();
    assertThat(verifier.verify(emptyPayload, Optional.of(standardHeader)))
        .isPresent()
        .hasValueSatisfying(msg -> assertThat(msg).contains("Invalid base64 payload"));
  }
}
