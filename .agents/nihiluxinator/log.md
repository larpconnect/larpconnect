## 2024-05-19 - Discovered `PORT` Environment Variable Configuration

**Learning:** The `web.port` can also be configured using the standard `PORT` environment variable
(which takes precedence over the `config.json` file configuration) as implemented in
`ServerBindingModule.java`. This is a common pattern for cloud deployments (like Render or Heroku)
but is currently undocumented in `docs/configuration.md`.

**Action:** Update the documentation to reflect that the `PORT` environment variable can be used to
set the `web.port` and takes precedence over `config.json` values.
