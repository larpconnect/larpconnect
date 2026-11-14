package com.larpconnect.njall.api.studios.inbox;

import java.util.Optional;

/**
 * Verifier contract for HTTP Content-Digest headers according to RFC 9530 supporting sha-256 and
 * sha3-256.
 */
interface ContentDigestVerifier {

  /**
   * Verifies the payload bytes against the optional Content-Digest header value.
   *
   * @param payloadBytes The raw request body bytes.
   * @param headerValue Optional Content-Digest header string.
   * @return Optional containing an error description if verification fails, or empty if valid.
   */
  Optional<String> verify(byte[] payloadBytes, Optional<String> headerValue);

  /** Computes an RFC 9530 formatted Content-Digest header for supported SHA-256 / SHA3-256. */
  String computeDigest(byte[] payloadBytes, String algorithm);
}
