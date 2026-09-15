Feature: Flyway Database Migration and Bootstrap Provisioning
  As a system administrator or deployment process
  I want to execute Flyway database migrations via the command-line or migration service
  So that the PostgreSQL database schemas, roles, enums, tables, and permissions are correctly provisioned

  Scenario: Bootstrap migration creates schemas and applies privileges
    Given a clean PostgreSQL database instance with pre-configured roles "njall", "njall_admin", "njall_users", and "njall_system"
    When the database migrator executes the bootstrap migration
    Then schemas "njall", "njall_admin", "njall_users", and "njall_system" exist and are owned by "njall"
    And role "njall_admin" has USAGE on "njall", "njall_admin", "njall_users" and SELECT on all tables in "njall"
    And role "njall_users" has USAGE on "njall", "njall_users" and SELECT on all tables in "njall"

  Scenario: Seed records are populated with configured placeholders
    Given migration configuration with server name "alpha-node", primary domain "larpconnect.test", and admin contact "ops@larpconnect.test"
    When the database migrator executes the bootstrap migration
    Then table "njall.servers" contains a server with name "alpha-node" and primary domain "larpconnect.test"
    And table "njall.server_contacts" contains an ADMIN contact with email "ops@larpconnect.test"

  Scenario: Server application executes migration and exits when migrate subcommand is provided
    Given the server application is started with argument "migrate"
    When the server execution completes
    Then database migrations are executed to completion
    And the application process terminates with exit status 0

  Scenario: Server application executes migration with custom database parameters
    Given the server application is started with custom database migration arguments
    When the server execution completes with custom arguments
    Then database migrations are executed to completion
    And the application process terminates with exit status 0

  Scenario: Bootstrap migration is idempotent
    Given the bootstrap database migration has already been executed
    When the database migrator executes the bootstrap migration again
    Then zero migrations are applied
