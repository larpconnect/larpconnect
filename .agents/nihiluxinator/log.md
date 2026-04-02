## 2024-05-19 - Discovered `PORT` Environment Variable Configuration

**Learning:** The `web.port` can also be configured using the standard `PORT` environment variable
(which takes precedence over the `config.json` file configuration) as implemented in
`ServerBindingModule.java`. This is a common pattern for cloud deployments (like Render or Heroku)
but is currently undocumented in `docs/configuration.md`.

**Action:** Update the documentation to reflect that the `PORT` environment variable can be used to
set the `web.port` and takes precedence over `config.json` values.

## 2024-05-19 - JSON Configuration keys must match Protobuf definitions

**Learning:** When configuring the application via `config.json`, the configuration keys must map to standard Protobuf JSON representation of the properties defined in `.proto` files (e.g., `webPort` or `web_port` instead of `web.port`), because Guice uses `JsonFormat.parser().ignoringUnknownFields().merge()` to deserialize the JSON into `LarpConnectConfig`. Unknown fields like `web.port` are silently ignored.

**Action:** Always ensure that configuration documentation specifies keys that match the camelCase (or snake_case) JSON representation of the underlying Protobuf message fields.

## 2026-03-27 - Test version mismatch in docs

**Learning:** Documentation across `CONTRIBUTING.md`, `AGENTS.md` and `.agents/skills/testing.md` stated that the project uses JUnit 5. However, `gradle/libs.versions.toml` specifies JUnit 6 (`6.0.3`) and Pentiousinator explicitly enforced JUnit 6. The documentation was factually out of date.

**Action:** Always ensure documentation referencing testing libraries correctly states "JUnit 6" to avoid confusing contributors with outdated major versions.

## 2024-05-19 - Do not document incorrect configuration key formats

**Learning:** A PR to document incorrect JSON configuration keys (like `web.port` or `web_port`) was rejected because it is assumed developers understand basic JSON and the expected format is already specified in a protobuf. Documenting "millions of ways it could be done but that won't work" is discouraged.

**Action:** When documenting configuration, focus only on how to do it correctly and rely on existing authoritative documentation (like protobufs) rather than enumerating negative examples.
