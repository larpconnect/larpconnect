# LARPConnect: Connecting LARP Communities

> [!CAUTION]
> **DEVELOPMENT STATUS: PRE-ALPHA**
> This project is **not** ready for production traffic. **DO NOT USE IT IN PRODUCTION.** Core security, stability, and data integrity features are still under active development.


[![Build](https://github.com/larpconnect/larpconnect/actions/workflows/build.yml/badge.svg)](https://github.com/larpconnect/larpconnect/actions/workflows/build.yml)

---

## Project Overview

**LARPConnect** is a platform designed to bridge and federate Live Action Role-Playing (LARP) communities. It allows for the creation of interconnected subcommunities that can communicate and share resources.

Think of it as a Discord-like or Mastodon-like experience tailored specifically for the needs of LARPers, with a heavy focus on:

* **Community Federation:** Connecting disparate groups while allowing for local subcommunity autonomy.
* **Data and Metadata Management:** Tracking complex game data, character information, and announcements from staff in a usable, searchable format.
* **Logistics & Signups:** Integrated tools for event registration and resource tracking.

---

## Technical Specifications

The project is a multi-module Gradle build designed for high performance and type safety.

* **Language:** Java 25
* **Framework:** Vert.x 5
* **Build System:** Gradle (Multi-module)
* **Main Entry Point:** `:server` 

---

## Getting Started

### Prerequisites
* **JDK 25** or higher.
* **Gradle 9.3+** (via the included wrapper).

### Build Instructions
To compile the project and run the test suite:

```bash
./gradlew build
```

### Updating OpenAPI Specs
To regenerate the OpenAPI specification (`openapi.yaml`) from Protobuf definitions and copy it to `src/main/resources` where it should be checked into version control, run:

```bash
./gradlew :proto:updateOpenApi
```

### Running Locally

To run the server directly via Gradle:

```bash
./gradlew :server:run
```

### Configuration

LarpConnect's configuration can be modified through `config.json`. For detailed information on
configuration settings, namespacing, and specifying custom configuration files, please refer to the
[Configuration Guide](docs/configuration.md).

### Creating a Fatjar

To create a standalone executable jar (fatjar):

```bash
./gradlew :server:shadowJar
```

To run the resulting fatjar:

```bash
java -jar server/build/libs/larpconnect.jar
```

### Running with Docker

To build the Docker image:

```bash
docker build -t larpconnect-server .
```

To run the container:

```bash
docker run -d -p 8080:8080 --name larpconnect larpconnect-server
```

To verify the service is running:

```bash
curl -v http://localhost:8080/v1/message
```

## License

All source code and comments licensed under Apache 2.0

See [LICENSE](./LICENSE) for more details. 

Not affiliated with or endorsed by Google LLC in any way.

Exceptions are as follows:

* Use of Generative AI in Contributions is licensed under Creative Commons Attribution-ShareAlike 4.0 International. To view a copy of this license, visit https://creativecommons.org/licenses/by-sa/4.0/
* The contents of the `docs/` folder is licensed under Creative Commons Attribution 4.0 International. To view a copy of this license, visit https://creativecommons.org/licenses/by/4.0/.
