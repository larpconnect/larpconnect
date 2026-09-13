# logger-conventions Specification

## Purpose

Enforces codebase consistency and SLF4J logger declaration standards across all Java components in Project Njall, preventing regression via automated Checkstyle analysis and requiring explicit class-literal instance loggers.

## Requirements

### Requirement: Forbid uppercase LOGGER and LOG variable names
The build system SHALL enforce via Checkstyle that no variable whose type is `Logger` is named `LOGGER` or `LOG`.

#### Scenario: Variable named LOGGER fails Checkstyle verification
- **GIVEN** a Java class declaring a field or variable of type `Logger` named `LOGGER`
- **WHEN** Checkstyle static analysis (`checkstyleMain` or `checkstyleTest`) is executed
- **THEN** Checkstyle SHALL fail with a violation stating that the variable must not be named 'LOGGER'.

#### Scenario: Variable named LOG fails Checkstyle verification
- **GIVEN** a Java class declaring a field or variable of type `Logger` named `LOG`
- **WHEN** Checkstyle static analysis (`checkstyleMain` or `checkstyleTest`) is executed
- **THEN** Checkstyle SHALL fail with a violation stating that the variable must not be named 'LOG'.

#### Scenario: Variable named logger passes Checkstyle verification
- **GIVEN** a Java class declaring a field or variable of type `Logger` named `logger`
- **WHEN** Checkstyle static analysis (`checkstyleMain` or `checkstyleTest`) is executed
- **THEN** Checkstyle SHALL pass without any logger naming violations.

#### Scenario: Static variable named logger is not rejected by Checkstyle rule
- **GIVEN** a Java class declaring a static field of type `Logger` named `logger`
- **WHEN** Checkstyle static analysis (`checkstyleMain` or `checkstyleTest`) is executed
- **THEN** the logger naming Checkstyle rule SHALL NOT produce a violation for that declaration.

### Requirement: Standardize Logger Instantiation on Class Literal
All Java classes utilizing an SLF4J `Logger` SHALL instantiate the logger using an instance field initialized with the explicit enclosing class literal (`LoggerFactory.getLogger(Foo.class)`).

#### Scenario: Non-static logger initialization in route classes
- **GIVEN** an HTTP route class `DefaultAdminRoute`
- **WHEN** its logger field is declared
- **THEN** it SHALL be declared as `private final Logger logger = LoggerFactory.getLogger(DefaultAdminRoute.class)`.

#### Scenario: Class literal logger initialization in services and entrypoints
- **GIVEN** an application service or entrypoint such as `ServerApp` or `DefaultHttpServerService`
- **WHEN** its logger field is declared
- **THEN** it SHALL be initialized using its enclosing class literal rather than virtual dispatch on `getClass()`.
