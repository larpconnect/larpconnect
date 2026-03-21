## 2024-05-19 - Discovered `PORT` Environment Variable Configuration

**Learning:** The `web.port` can also be configured using the standard `PORT` environment variable
(which takes precedence over the `config.json` file configuration) as implemented in
`ServerBindingModule.java`. This is a common pattern for cloud deployments (like Render or Heroku)
but is currently undocumented in `docs/configuration.md`.

**Action:** Update the documentation to reflect that the `PORT` environment variable can be used to
set the `web.port` and takes precedence over `config.json` values.

## 2026-03-21 - Protobuf JSON Formatting

**Learning:** Protobuf JsonFormat requires JSON keys to match standard camelCase names (e.g., `webPort`, `openapiSpec`) instead of dot notation.

**Action:** Update all documentation to use camelCase configuration property names instead of dot notation to correctly map to LarpConnectConfig protobuf fields.
