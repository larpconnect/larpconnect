# LarpConnect: Connecting Larp Communities

> [!CAUTION]
> **DEVELOPMENT STATUS: PRE-ALPHA** This project is **not** ready
> for production traffic. **DO NOT USE IT IN PRODUCTION.** Core security,
> stability, and data integrity features are still under active development.

[![Build](https://github.com/larpconnect/larpconnect/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/larpconnect/larpconnect/actions/workflows/build.yml)

---

## Project Overview

**LarpConnect** is a platform designed to bridge and federate Live Action
Role-Playing (LARP) communities. It allows for the creation of interconnected
subcommunities that can communicate and share resources.

Think of it as a Discord-like or Mastodon-like experience tailored specifically
for the needs of LARPers, with a heavy focus on:

- **Community Federation:** Connecting disparate groups while allowing for local
  subcommunity autonomy.
- **Data and Metadata Management:** Tracking complex game data, character
  information, and announcements from staff in a usable, searchable format.
- **Logistics & Signups:** Integrated tools for event registration and resource
  tracking.

---

## Development

To build the project run:

```java
./gradlew build
```

To launch a local server for development you can use `./gradlew composeStart` and then shut down with `./gradlew composeClean` (or `./gradlew composeDown` to shut it down but preserve its state for the next start).

---

## Technical Specifications

The project is a multi-module Gradle build designed for high performance and
type safety.

- **Language:** Java 25
- **Framework:** Pekko
- **Build System:** Gradle (Multi-module)
- **Main Entry Point:** `:server`

---

## Notes on Agentic LLM Development Methodology

This methodology is **NOT REQUIRED**. The only thing is that if you are going to use an LLM it is requested, though not required, that you work with OpenSpec to ensure consistency.

This is in part being done as an experiment in improving the way we develop using
LLMs. While on the overall this is a project with a goal and a mission, the primary
developer is also using it as a testbed for agentic engineering. 

This is a counter to the observed pattern of "turning off your brain" and "just approving what the LLM generates" and instead experimetning with different patterns of development to try to find a methodology that allows humans to work on the harder and more interesting problems without compromising code quality.

To that end the
methodology being experimented with is as follows:

* A focus on specifications. Changes are done using OpenSpec using intent-driven
  development. This involves:
  - Generating a specification with a spec, design, proposal, and a list of tasks.
  - Documenting any major decisions using an ADR file.
  - Verifying the specifications on a regular basis to ensure that they are still valid,
    and keeping them up-to-date.
  - Syncing the specs after the implementation is finished
  - Archiving the specs after the sync is finished
* A focus on automated code quality tools and tests:
  - Checkstyle: Enforcing coding standards and best practices.
  - SpotBugs: Enforcing static analysis best practices.
  - ErrorProne: Enforcing compile-time error checking.
  - ArchUnit: Enforcing software architectural best practices.
  - Spotless: Code formatting to Google's java standards.
  - Cucumber: For end-to-end integration testing using Testcontainers.
  - Both local and remote enforcement mechanisms. The remote code quality checks keep
    it out of the hands of the LLM so that it cannot outrigth disable it.
  - Both local and remote code review (local with `/njall-review`, remote with github's code 
    review tools)
* Basic human code review for most code and intense human code review for database and security code. 
  - Stronger review on the specifications
  - "Don't sweat the small stuff" when it comes to problems in the generated code. If there is a 
    problem, even a minor problem, it should **still be fixed**, but rather the goal shifts from
    telling the LLM a list of small things to fix and creating overarching rules to follow.
* Periodic deeper code reviews across the system in which problems are identified and automated enforcement is put into place to prevent problems. 

This is an experiment to try to make programming with LLMs more tolerable while not compromising
significantly on quality. Basically trying to reduce the difficulty of review for a human and enforcing quality, as much as is practiceable, through automated testing. Including in the development and code style. 


---

## License

All source code and comments licensed under Apache 2.0

See [LICENSE](./LICENSE) for more details.
