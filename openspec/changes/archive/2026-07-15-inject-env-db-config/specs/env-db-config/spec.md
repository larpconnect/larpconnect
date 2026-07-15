## ADDED Requirements

### Requirement: Database Config Resolution from Environment
The system SHALL resolve database configuration properties from environment variables with fallbacks to defaults.

#### Scenario: Port is not provided
- **WHEN** the DB_PORT environment variable is not defined or is blank
- **THEN** the resolved database port SHALL default to 5432

#### Scenario: Port is invalid
- **WHEN** the DB_PORT environment variable is defined but is not a valid integer
- **THEN** the resolved database port SHALL default to 5432

#### Scenario: Configuration matches provided environment
- **WHEN** the environment defines DB_HOST as 'db-server' and DB_PORT as '5433'
- **THEN** the resolved database host SHALL be 'db-server' and the port SHALL be 5433
