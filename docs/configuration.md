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

Currently, LarpConnect supports the following configuration properties:

### `web.port`

- **Type:** Integer
- **Default Value:** 8080
- **Description:** This property specifies the network port that the LarpConnect
  web server will bind to and listen on for incoming HTTP requests. You can
  change this if port 8080 is already in use by another application on your
  system.

  > [!NOTE]
  > The `PORT` environment variable takes precedence over the `web.port` setting
  > in `config.json`. This is commonly used in cloud deployments. If `PORT` is
  > not set or not a valid integer, the application falls back to `web.port`.

### `openapi.spec`

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
    "web.port": 9000,
    "openapi.spec": "custom_openapi.yaml"
  }
}
```

If you do not provide a `config.json` file, or if the file is missing these
properties, the application will start using the default values (port 8080 and
`openapi.yaml`).

## How Configuration is Loaded

When LarpConnect starts, the entry point initializes the Vert.x framework and
Guice dependency injection. The configuration file is read and passed into the
Guice `ServerModule`. This module is responsible for providing the configuration
values to the various parts of the application, such as the `WebServerVerticle`,
which uses the `web.port` and `openapi.spec` settings to start the HTTP server.
