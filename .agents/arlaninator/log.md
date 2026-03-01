## 2024-05-24 - Insecure Random Number Generation for ID Creation

**Vulnerability:** The application was using `ThreadLocalRandom.current()` (a predictable PRNG) to provide the `RandomGenerator` bean, which is then used by `UuidV7Generator` for generating unique identifiers.
**Learning:** This could lead to predictable ID generation, allowing attackers to guess UUIDs.
**Prevention:** Use `new SecureRandom()` (a cryptographically secure PRNG) whenever generating sensitive values like IDs, tokens, or passwords to ensure unpredictability.
