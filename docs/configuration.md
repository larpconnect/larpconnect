# Configuration Guide

LarpConnect uses a JSON configuration file to manage application settings. The
main configuration file is typically named `config.json` and must be provided to
the application upon startup.

This document explains how to configure LarpConnect and the available settings.

## The `larpconnect` Namespace

All configuration settings specific to LarpConnect are contained within a
top-level JSON object named `larpconnect`. This ensures that LarpConnect
settings do not conflict with other configuration values that might be present
in the environment or provided by other tools.

The application reads the full JSON configuration object when it starts. The
main server module then extracts the settings from the `larpconnect` namespace.
If a setting is not found within the namespace, the application will attempt to
fall back to the root level of the configuration file, though placing settings
in the namespace is strongly recommended.

### Specifying the Configuration File

By default, LarpConnect attempts to load the configuration from a file named
`config.json` on the classpath. You can override this default behavior and
specify a custom configuration file location by setting the
`njall.config.resource` system property when starting the application.

For example, to load a configuration file named `custom-config.json`:

```bash
java -Dnjall.config.resource=custom-config.json -jar server/build/libs/larpconnect.jar
```

The specified resource must be available on the classpath.

## Supported Properties

The configuration settings are backed by the `LarpConnectConfig` Protobuf message defined
in `config.proto`. Therefore, configuration keys must map to the standard Protobuf JSON
representation of these properties (typically `camelCase` or `snake_case`). The system
deserializes the JSON configuration directly into this Protobuf message, and any unrecognized
keys will be silently ignored.

For the most authoritative list of available settings, always refer to the `LarpConnectConfig`
definition in `config.proto`. Currently, the core properties include:

### `webPort`

- **Type:** Integer
- **Default Value:** 8080
- **Description:** This property specifies the network port that the LarpConnect
  web server will bind to and listen on for incoming HTTP requests. You can
  change this if port 8080 is already in use by another application on your
  system.

  > [!NOTE]
  > The standard `PORT` environment variable takes precedence over the `webPort` setting
  > configured in `config.json`. This is implemented in `ServerBindingModule.java` and serves
  > as a common pattern for cloud deployments (such as Render or Heroku). If `PORT` is not set
  > or is not a valid integer, the application falls back to the `webPort` setting.

### `openapiSpec`

- **Type:** String
- **Default Value:** `openapi.yaml`
- **Description:** This property defines the path or name of the OpenAPI
  specification file that the server uses to configure its API routes and
  validation. The file must be accessible on the application's classpath.

## Configuration Example

Here is an example of a valid `config.json` file that sets the web server to run
on port 9000 and uses a custom OpenAPI specification file:

```json
{
  "larpconnect": {
    "webPort": 9000,
    "openapiSpec": "custom_openapi.yaml"
  }
}
```

If you do not provide a `config.json` file, or if the file is missing these
properties, the application will start using the default values (port 8080 and
`openapi.yaml`).

## How Configuration is Loaded

When LarpConnect starts, the entry point initializes the Vert.x framework and
Guice dependency injection. The configuration file is read and passed into the
Guice `ServerModule`. Behind the scenes, the configuration JSON is parsed and merged into a
strongly-typed `LarpConnectConfig` Protobuf object. This object is then injected throughout the
application, ensuring that components like the `WebServerVerticle` receive validated `webPort` and
`openapiSpec` settings to start the HTTP server safely.
